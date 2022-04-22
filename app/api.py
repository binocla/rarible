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


@app.post("/predict_price/")
async def predict_price(request: Request, response: Response):
    response.headers['Access-Control-Allow-Origin'] = '*'
    nft_json = await request.json()
    if not JSON_ATTRIBUTES.issubset(set(nft_json.keys())):
        raise HTTPException(status_code=400, detail='Error in input json!')

    try:
        nft_json['rare'] = float(nft_json['rare'])
    except:
        raise HTTPException(status_code=400, detail='Error in input json!')
    
    collection = nft_json['collection']
    nft_id = nft_json['id']
    rare = nft_json['rare']
    attributes = nft_json['attributes']

    predicted_price = 0

    return {'collection': collection, 'nft_id': nft_id, 'predicted_price': predicted_price}
    