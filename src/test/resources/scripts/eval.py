from sentence_transformers import SentenceTransformer, util

model = SentenceTransformer("all-MiniLM-L6-v2")

emb_actual = model.encode(df["actual"].tolist(), convert_to_tensor=True)
emb_expected = model.encode(df["expected"].tolist(), convert_to_tensor=True)

cosine_scores = util.cos_sim(emb_actual, emb_expected)
# Diagonal gives pairwise scores
pairwise_scores = cosine_scores.diagonal().cpu().tolist()

print(f"Average similarity: {sum(pairwise_scores)/len(pairwise_scores):.4f}")
