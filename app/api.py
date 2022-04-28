import asyncio
from gc import collect
import json
import os
import re
import string
import traceback
import faiss

import numpy as np
import pandas as pd
from fastapi import FastAPI, HTTPException, Response, Request
from sentence_transformers import SentenceTransformer
from sklearn.cluster import KMeans

from app.logging import get_logger

logger = get_logger(__name__)

app = FastAPI()

data = {
    'model': None,
    'kmeans': None,
    'attr_clusters': None,
    'nft_df': None,
    'faiss_index': None
}


JSON_PATH = 'data/nft50k.json'
JSON_ATTRIBUTES = {
    'collection',
    'id',
    'rare',
    'attributes'
}
KMEANS_CLUSTERS = 20
ANN_K = 10
Y_COL = 'makePriceUsd'


@app.post("/load")
async def load(response: Response):
    response.headers['Access-Control-Allow-Origin'] = '*'

    logger.info('Loading json...')
    with open(JSON_PATH, 'r', ) as f:
        rows = json.load(f)
    
    nft_df = pd.DataFrame(rows)
    nft_df['makePriceUsd'] = nft_df['makePriceUsd'].astype('float64')
    nft_df['attributes_count'] = nft_df['attributes'].apply(len)
    nft_df = nft_df[nft_df['rare'] > 0]

    collection_rare = nft_df.groupby('collection')['rare'].mean()

    nft_df = nft_df.merge(collection_rare, on='collection', how='left', suffixes=('', '_collection'))

    nft_df = nft_df.set_index('id')
    nft_df = nft_df.sample(frac=1)
    logger.info('Json loaded!')

    logger.info('Loading attr clusters...')
    attrs_all = []
    for id_, attrs in nft_df.attributes.iteritems():
        for attr in attrs:
            attrs_all.append(attr['key'].lower())
    attrs_all = list(set(attrs_all))


    model = SentenceTransformer('paraphrase-MiniLM-L6-v2')
    attrs_embeddings = model.encode(attrs_all)
    kmeans = KMeans(n_clusters=KMEANS_CLUSTERS, random_state=0).fit(attrs_embeddings)
    attr_clusters = dict(zip(attrs_all, kmeans.labels_))

    data['model'] = model
    data['kmeans'] = kmeans
    data['attr_clusters'] = attr_clusters

    logger.info('Clusters loaded...')

    logger.info('Loading attr vectors...')
    attrs_vectors = {}
    for id_, attrs in nft_df.attributes.iteritems():
        attr_vector = np.full((KMEANS_CLUSTERS,), 0, dtype='float')
        
        for attr in attrs:
            attr_key = attr['key'].lower()
            attr_rare = attr['rare']
            
            attr_index = attr_clusters[attr_key]
            
            attr_vector[attr_index] += attr_rare
        
        attrs_vectors[id_] = attr_vector

    attrs_vectors = pd.DataFrame(attrs_vectors).T
    attrs_vectors.columns = [f'cluster_{c + 1}'for c in attrs_vectors.columns]

    attrs_vectors['rare'] = nft_df['rare']
    attrs_vectors['attributes_count'] = nft_df['attributes_count']

    attrs_vectors = attrs_vectors[~nft_df['makePriceUsd'].isnull()]
    nft_df = nft_df[~nft_df['makePriceUsd'].isnull()]

    attrs_vectors.to_csv('data/attrs_vectors.csv', index=True)
    nft_df.to_csv('data/nft_df.csv', index=True)

    data['nft_df'] = nft_df
    logger.info('Attr vectors loaded...')

    logger.info('Creating index...')
    faiss_index = faiss.IndexFlatL2(KMEANS_CLUSTERS + 2)
    faiss_index.add(np.ascontiguousarray(np.array(attrs_vectors).astype(np.float32)))
    data['faiss_index'] = faiss_index
    logger.info(f'Index created {faiss_index.ntotal}!')

    return {'status': 'ok'}


@app.post("/predict_price")
async def predict_price(request: Request, response: Response):
    response.headers['Access-Control-Allow-Origin'] = '*'

    if data['faiss_index'] is None:
        raise HTTPException(status_code=400, detail='Index is not created!')

    nft_json = await request.json()
    if not JSON_ATTRIBUTES.issubset(set(nft_json.keys())):
        raise HTTPException(status_code=400, detail='Error in input json!')

    try:
        nft_json['rare'] = float(nft_json['rare'])
    except:
        raise HTTPException(status_code=400, detail='Error in input json!')

    # prepare vector
    collection = nft_json['collection']
    nft_id = nft_json['id']
    attributes = nft_json['attributes']
    nft_rare = nft_json['rare']
    attributes_count = len(attributes)

    attr_vector = np.full((KMEANS_CLUSTERS + 2,), -1, dtype=np.float32)
    for attr in attributes:
        attr_key = attr['key'].lower()
        attr_rare = attr['rare']

        attr_index = data['attr_clusters'].get(attr_key, -1)

        if attr_index == -1:
            attr_key_vector = data['model'].encode(attr_key)[np.newaxis, :]
            attr_index = data['model'].kmeans.predict(attr_key_vector)[0]
        
        attr_vector[attr_index] += attr_rare
    attr_vector[-2] = nft_rare
    attr_vector[-1] = attributes_count
    attr_vector = attr_vector.reshape(1, -1)
    attr_vector = np.ascontiguousarray(attr_vector).astype(np.float32)

    _, I = data['faiss_index'].search(attr_vector, ANN_K)
    predicted_price = data['nft_df'].iloc[I[0]][Y_COL].mean()

    return {'collection': collection, 'nft_id': nft_id, 'predicted_price': predicted_price}
    