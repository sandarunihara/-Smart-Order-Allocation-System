import os
import re
import joblib
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MODEL_DIR = os.path.join(BASE_DIR, "model")

try:
    classifier = joblib.load(os.path.join(MODEL_DIR, "classifier.pkl"))
    vectorizer = joblib.load(os.path.join(MODEL_DIR, "vectorizer.pkl"))
    MODEL_LOADED = True
except FileNotFoundError:
    classifier = None
    vectorizer = None
    MODEL_LOADED = False
    print("WARNING: Model files not found. Run train.py first.")

app = FastAPI(
    title="Smart Order — Message Classifier",
    description="Classifies customer messages into support categories",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

CATEGORIES = [
    "Payment Issue",
    "Delivery Issue",
    "Refund/Cancellation",
    "Product/Stock Inquiry",
    "Order Status Inquiry",
    "Account/Login Issue",
    "Promotion/Discount Inquiry",
    "General Inquiry",
]

CONFIDENCE_THRESHOLD = 0.60


class ClassifyRequest(BaseModel):
    message: str = Field(..., min_length=1, max_length=1000)


class ClassifyResponse(BaseModel):
    category: str
    confidence: float
    is_confident: bool
    all_scores: dict[str, float] | None = None


def clean_text(text: str) -> str:
    if not isinstance(text, str):
        return ""
    text = text.lower().strip()
    text = re.sub(r"[^a-z0-9\s?!.,']", "", text)
    text = re.sub(r"\s+", " ", text)
    return text


@app.get("/health")
def health():
    return {"status": "ok", "model_loaded": MODEL_LOADED}


@app.get("/categories")
def get_categories():
    return {"categories": CATEGORIES}


@app.post("/classify", response_model=ClassifyResponse)
def classify(request: ClassifyRequest):
    if not MODEL_LOADED:
        raise HTTPException(status_code=503, detail="Model not loaded. Run train.py first.")

    cleaned = clean_text(request.message)
    if not cleaned:
        raise HTTPException(status_code=400, detail="Message is empty after cleaning.")

    X = vectorizer.transform([cleaned])
    probabilities = classifier.predict_proba(X)[0]
    classes = classifier.classes_

    best_idx = probabilities.argmax()
    best_category = classes[best_idx]
    best_confidence = float(probabilities[best_idx])

    all_scores = {
        cls: round(float(prob), 4) for cls, prob in sorted(
            zip(classes, probabilities), key=lambda x: x[1], reverse=True
        )
    }

    is_confident = best_confidence >= CONFIDENCE_THRESHOLD

    return ClassifyResponse(
        category=best_category if is_confident else "Uncertain",
        confidence=round(best_confidence * 100, 2),
        is_confident=is_confident,
        all_scores=all_scores,
    )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
