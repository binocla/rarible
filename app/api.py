import asyncio
from gc import collect
import json
import os
import re
import string
import traceback

import numpy as np
import pandas as pd
from fastapi import FastAPI, HTTPException, Response, Request

from app.logging import get_logger

logger = get_logger(__name__)

app = FastAPI()


JSON_ATTRIBUTES = {
    'collection',
    'id',
    'rare',
    'attributes'
}


@app.post("/predict_price")
async def predict_price(request: Request, response: Response):
    response.headers['Access-Control-Allow-Origin'] = '*'
    nft_json = await request.json()
    if not JSON_ATTRIBUTES.issubset(set(nft_json.keys())):
        raise HTTPException(status_code=400, detail='Error in input json!')

    try:
        nft_json['rare'] = float(nft_json['rare'])
    except:
        raise HTTPException(status_code=400, detail='Error in input json!')

    # load vectors
    nfts = pd.read_csv('./data/nft.csv', index_col=0)
    attrs_all = nfts.columns[:-3].tolist()
    n = len(attrs_all)

    # prepare vector
    collection = nft_json['collection']
    nft_id = nft_json['id']
    attributes = nft_json['attributes']
    nft_rare = nft_json['rare']
    attributes_count = len(attributes)

    attr_vector = np.full((n + 2,), -1, dtype='float')
    for attr in attributes:
        attr_key = attr['key'].lower()
        attr_rare = attr['rare']
        attr_index = attrs_all.index(attr_key)
        attr_vector[attr_index] = attr_rare
    attr_vector[-2] = nft_rare
    attr_vector[-1] = attributes_count

    d = np.sum((nfts.values[:, :-1] - attr_vector)**2, axis=1)
    k_idx = np.argsort(d)[1:11]
    predicted_price = np.mean(nfts.iloc[k_idx]['y'].values)

    return {'collection': collection, 'nft_id': nft_id, 'predicted_price': predicted_price}
    