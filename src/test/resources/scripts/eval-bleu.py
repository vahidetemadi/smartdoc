import sacrebleu

# expected = references, actual = candidates
bleu = sacrebleu.corpus_bleu(actual, [expected])
print(f"SacreBLEU: {bleu.score:.2f}")
