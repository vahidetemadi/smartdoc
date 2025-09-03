import pandas as pd
from bert_score import score


df = pd.read_csv(
    ...:     "things.txt",
    ...:     sep=",",
    ...:     quotechar='"',
    ...:     engine="python",
    ...:     skipinitialspace=True,
    ...:     on_bad_lines="skip",
    ...: )



actual = [str(x).strip().replace("\n", " ") for x in df["actual"]]
expected = [str(x).strip().replace("\n", " ") for x in df["expected"]]

p, r, f1 = score(
    actual,
    expected,
    model_type="roberta-large",  # or microsoft/codebert-base for code
    lang="en",
    verbose=True,
    idf=False,
    rescale_with_baseline=False,
    all_layers=False
)
