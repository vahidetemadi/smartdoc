##### Helpers for super metrics #############
#### for Bert:
def getBertScore(project, model):
    import pandas as pd
    from bert_score import score

    df = pd.read_csv(f"../testResults/{model}/{project}.txt",
                     sep=",", quotechar='"', engine="python",
                     skipinitialspace=True, on_bad_lines="skip")

    if "actual" not in df.columns or "expected" not in df.columns:
        raise ValueError("CSV must have 'actual' and 'expected' columns")

    actual = [str(x).strip().replace("\n", " ") for x in df["actual"]]
    expected = [str(x).strip().replace("\n", " ") for x in df["expected"]]

    P, R, F1 = score(actual, expected,
                     model_type="roberta-large",
                     lang="en",
                     verbose=True,
                     idf=False,
                     rescale_with_baseline=False,
                     all_layers=False)

    # Return averages as floats
    return P.mean().item(), R.mean().item(), F1.mean().item()

###### For Blue ######
def getBleuScore(project, model):
    import pandas as pd
    import sacrebleu

    df = pd.read_csv(f"../testResults/{model}/{project}.txt",
                     sep=",", quotechar='"', engine="python",
                     skipinitialspace=True, on_bad_lines="skip")

    if "actual" not in df.columns or "expected" not in df.columns:
        raise ValueError("CSV must have 'actual' and 'expected' columns")

    actual = [str(x).strip().replace("\n", " ") for x in df["actual"]]
    expected = [str(x).strip().replace("\n", " ") for x in df["expected"]]

    bleu = sacrebleu.corpus_bleu(actual, [expected])

    # Return averages as floats
    return bleu.score

###### For Rouge ######
def getRougeScore(project, model):
    import pandas as pd
    from rouge_score import rouge_scorer

    df = pd.read_csv(f"../testResults/{model}/{project}.txt",
                     sep=",", quotechar='"', engine="python",
                     skipinitialspace=True, on_bad_lines="skip")

    if "actual" not in df.columns or "expected" not in df.columns:
        raise ValueError("CSV must have 'actual' and 'expected' columns")

    actual = [str(x).strip().replace("\n", " ") for x in df["actual"]]
    expected = [str(x).strip().replace("\n", " ") for x in df["expected"]]

    scorer = rouge_scorer.RougeScorer(["rouge1", "rouge2", "rougeL"], use_stemmer=True)
    scores = [scorer.score(ref, cand) for ref, cand in zip(expected, actual)]

    avg_precision = sum(s["rouge1"].precision for s in scores) / len(scores)
    avg_recall = sum(s["rouge1"].recall for s in scores) / len(scores)
    avg_f1 = sum(s["rouge1"].fmeasure for s in scores) / len(scores)

    print(avg_precision)
    print(avg_recall)
    print(avg_f1)
    return avg_precision, avg_recall, avg_f1

######### Define the schema #############
import sys

super_metric = sys.argv[1]
super_metric = super_metric.strip().upper()

print("Reached here")
metrics = []
match super_metric:
    case "BERT":
        metrics = ["Precision", "Recall", "F1-measure"]
    case "ROUGE":
        metrics = ["Precision", "Recall", "F1-measure"]
    case "BLEU":
        metrics = ["Bleu"]

projects = ["edge", "gateway", "json", "things", "thingsearch"]
models = ["DEEPSEEK_REMOTE_CODER", "DEEPSEEK_REMOTE_REASONER"]

print(metrics)
data = {metric: {} for metric in metrics}

for project in projects:
    for model in models:
        match super_metric:
            case "BERT":
                p, r, f1 = getBertScore(project, model)
                data["Precision"].setdefault(project, {})[model] = p
                data["Recall"].setdefault(project, {})[model] = r
                data["F1-measure"].setdefault(project, {})[model] = f1
            case "BLEU":
                bleu = getBleuScore(project, model)
                data["Bleu"].setdefault(project, {})[model] = bleu
            case "ROUGE":
                p, r, f1 = getRougeScore(project, model)
                data["Precision"].setdefault(project, {})[model] = p
                data["Recall"].setdefault(project, {})[model] = r
                data["F1-measure"].setdefault(project, {})[model] = f1



######### Prepare the df to be visualized ##########

import pandas as pd

records = []

for metric, projects_dict in data.items():
    for project, model_dict in projects_dict.items():
        for model, value in model_dict.items():
            records.append(
                {
                "metric": metric,
                "project": project,
                "model": model,
                "value": value
                }
            )

df = pd.DataFrame(records)

############ Visualize the metrics #############

import seaborn as sns
import matplotlib.pyplot as plt

for metric in df['metric'].unique():
    subset = df[df['metric'] == metric]
    plt.figure(figsize=(8, 4))
    sns.barplot(data=subset, x="project", y="value", hue="model")
    plt.title(f"{metric.capitalize()}")
    plt.grid(axis='y', linestyle='--', alpha=0.6)
    plt.legend(title="Model", bbox_to_anchor=(1.05, 1), loc='upper left')
    plt.tight_layout()
    plt.savefig(f"{super_metric}_{metric}_comparison.pdf")

