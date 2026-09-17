import os
import re
import pandas as pd
import joblib
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.svm import LinearSVC
from sklearn.calibration import CalibratedClassifierCV
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import classification_report

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_PATH = os.path.join(BASE_DIR, "..", "Customer_Message_Dataset.csv")
MODEL_DIR = os.path.join(BASE_DIR, "model")
os.makedirs(MODEL_DIR, exist_ok=True)

def clean_text(text: str) -> str:
    if not isinstance(text, str):
        return ""
    text = text.lower().strip()
    text = re.sub(r"[^a-z0-9\s?!.,']", "", text)
    text = re.sub(r"\s+", " ", text)
    return text

def main():
    print("=" * 60)
    print("Customer Message Classifier — Training")
    print("=" * 60)

    df = pd.read_csv(DATA_PATH)
    print(f"\nRaw dataset: {len(df)} rows")

    df = df.dropna(subset=["message", "category"])
    df = df[df["message"].str.strip() != ""]
    df = df[df["category"].str.strip() != ""]
    print(f"After cleaning: {len(df)} rows")

    print("\nCategory distribution:")
    print(df["category"].value_counts().to_string())

    df["clean_message"] = df["message"].apply(clean_text)

    X = df["clean_message"].values
    y = df["category"].values

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )
    print(f"\nTrain: {len(X_train)} | Test: {len(X_test)}")

    vectorizer = TfidfVectorizer(
        max_features=5000,
        ngram_range=(1, 2),
        min_df=2,
        max_df=0.95,
        sublinear_tf=True,
    )
    X_train_tfidf = vectorizer.fit_transform(X_train)
    X_test_tfidf = vectorizer.transform(X_test)

    base_svm = LinearSVC(C=1.0, max_iter=10000, random_state=42)
    model = CalibratedClassifierCV(base_svm, cv=5)
    model.fit(X_train_tfidf, y_train)

    y_pred = model.predict(X_test_tfidf)
    print("\n" + "=" * 60)
    print("Classification Report")
    print("=" * 60)
    print(classification_report(y_test, y_pred))

    X_all_tfidf = vectorizer.transform(X)
    cv_scores = cross_val_score(
        CalibratedClassifierCV(LinearSVC(C=1.0, max_iter=10000, random_state=42), cv=5),
        X_all_tfidf, y, cv=5, scoring="accuracy"
    )
    print(f"5-Fold CV Accuracy: {cv_scores.mean():.4f} (+/- {cv_scores.std():.4f})")

    joblib.dump(model, os.path.join(MODEL_DIR, "classifier.pkl"))
    joblib.dump(vectorizer, os.path.join(MODEL_DIR, "vectorizer.pkl"))
    print(f"\nModel saved to {MODEL_DIR}/")
    print("Done!")

if __name__ == "__main__":
    main()
