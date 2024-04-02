#!/usr/bin/python3

from argparse import ArgumentParser
import itertools

import numpy as np
import joblib

from sklearn.preprocessing import OrdinalEncoder

from GlobalConstants import EMBEDDINGS_CNT
from GraphDataloader import load_data


def create_ordinal_encoder(paths_to_datasets: list[str], path_to_ordinal_encoder: str):
    data = load_data(paths_to_datasets, "train", 16)

    encoder = OrdinalEncoder(
        dtype=int,
        handle_unknown="use_encoded_value", unknown_value=EMBEDDINGS_CNT - 1,
        encoded_missing_value=EMBEDDINGS_CNT - 2
    )

    print("fitting ordinal encoder")
    encoder.fit(np.array(list(itertools.chain(
        *(list(zip(*data))[0])
    ))).reshape(-1, 1))

    for cat in encoder.categories_:
        if len(cat) > EMBEDDINGS_CNT - 2:
            print("w: too many categories")

    print("dumping ordinal encoder")
    joblib.dump(encoder, path_to_ordinal_encoder)

    with open(path_to_ordinal_encoder + ".cats", "w") as f:
        for sample in encoder.categories_[0]:
            f.write(str(sample) + "\n")


def get_args():
    parser = ArgumentParser(description="ordinal encoder preparing script")
    parser.add_argument("--ds", required=True, nargs="+")
    parser.add_argument("--oenc", required=True)

    args = parser.parse_args()
    print("args:")
    for arg in vars(args):
        print(arg, "=", getattr(args, arg))

    print()

    return args


if __name__ == "__main__":
    args = get_args()

    create_ordinal_encoder(args.ds, args.oenc)
