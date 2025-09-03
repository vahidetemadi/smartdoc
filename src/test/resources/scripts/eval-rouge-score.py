from rouge_score import rouge_scorer

scorer = rouge_scorer.RougeScorer(['rouge1', 'rouge2', 'rougeL'], use_stemmer=True)

actual = df["actual"].apply(lambda x: " ".join(str(x).split()))
expected = df["expected"].apply(lambda x: " ".join(str(x).split()))

scores = [scorer.score(ref, cand) for ref, cand in zip(expected, actual)]
