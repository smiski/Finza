# Caleb Mercier - 2025
# Completed with assistance from Chat GPT

from fastapi import FastAPI, HTTPException, Depends
from fastapi.security import OAuth2PasswordBearer
from pydantic import BaseModel
from sqlalchemy.orm import Session
from typing import List

from database import SessionLocal, User
from security import (
    hash_password,
    verify_password,
    create_access_token,
    decode_access_token,
)
from monte_carlo import run_simulation_from_payload
from rec_model import recommend_for_payload   # ⬅️ NEW

app = FastAPI()

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="login")

# ---------------------------
# DB dependency
# ---------------------------

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


# ---------------------------
# Auth models & routes
# ---------------------------

class UserCreate(BaseModel):
    username: str
    password: str


class UserLogin(BaseModel):
    username: str
    password: str


@app.post("/register")
def register(user: UserCreate, db: Session = Depends(get_db)):
    existing = db.query(User).filter(User.username == user.username).first()
    if existing:
        raise HTTPException(status_code=400, detail="Username already exists")

    hashed_pw = hash_password(user.password)
    new_user = User(username=user.username, password_hash=hashed_pw)
    db.add(new_user)
    db.commit()
    db.refresh(new_user)
    return {"message": f"User {new_user.username} created successfully"}


@app.post("/login")
def login(user: UserLogin, db: Session = Depends(get_db)):
    db_user = db.query(User).filter(User.username == user.username).first()
    if not db_user or not verify_password(user.password, db_user.password_hash):
        raise HTTPException(status_code=400, detail="Invalid username or password")

    token = create_access_token({"sub": db_user.username})
    return {"message": f"Hello {db_user.username}!", "access_token": token}


# ---------------------------
# Monte Carlo request/response models
# ---------------------------

class AssetReturn(BaseModel):
    mean: float
    volatility: float


class Returns(BaseModel):
    cash: AssetReturn
    taxable: AssetReturn
    tax_deferred: AssetReturn
    tax_free: AssetReturn


class Portfolio(BaseModel):
    cash: float
    taxable: float
    tax_deferred: float
    tax_free: float


class AnnualSavings(BaseModel):
    cash: float
    taxable: float
    tax_deferred: float
    tax_free: float


class Individual(BaseModel):
    name: str
    current_age: int
    retirement_age: int
    planning_horizon: int
    pre_retirement_income: float
    post_retirement_income: float
    portfolio: Portfolio
    annual_savings: AnnualSavings


class RetirementInput(BaseModel):
    individuals: List[Individual]
    pre_retirement_expenses: float
    post_retirement_expenses: float
    returns: Returns
    inflation: float = 0.025
    num_simulations: int = 1000


class SimulationResult(BaseModel):
    years: List[int]
    total_balances: List[float]
    success_probability: float
    average_ending_balance: float
    ending_balance_plus_1sd: float
    ending_balance_minus_1sd: float
    average_funds_last_age: float
    ages_per_year: List[List[str]]


# Your Kotlin client expects this *flat* shape,
# so SimulationResponse is just an alias here.
SimulationResponse = SimulationResult


# ---------------------------
# Recommendation models
# ---------------------------

class Recommendation(BaseModel):
    id: int
    title: str
    description: str
    impact_summary: str


class RecommendationResponse(BaseModel):
    recommendations: List[Recommendation]


# ---------------------------
# /simulate route
# ---------------------------

@app.post("/simulate", response_model=SimulationResponse)
def simulate(
    payload: RetirementInput,
    token: str = Depends(oauth2_scheme),
    db: Session = Depends(get_db),
):
    """
    Run the Monte Carlo simulation.

    - Expects a JSON body matching RetirementInput (same as KMP `RetirementInput`)
    - Requires Authorization: Bearer <token>
    - Returns JSON matching KMP `SimulationResponse` (flat)
    """
    # 1) Validate token
    username = decode_access_token(token)
    if not username:
        raise HTTPException(status_code=401, detail="Invalid or expired token")

    # 2) Run simulation
    try:
        # monte_carlo.run_simulation_from_payload returns a flat dict:
        # {
        #   "years": [...],
        #   "total_balances": [...],
        #   "success_probability": ...,
        #   "average_ending_balance": ...,
        #   "ending_balance_plus_1sd": ...,
        #   "ending_balance_minus_1sd": ...,
        #   "average_funds_last_age": ...,
        #   "ages_per_year": [...]
        # }
        raw_result = run_simulation_from_payload(payload.dict(), payload.num_simulations)

        # Pydantic validation – if keys/types don't match, this will raise
        sim_result = SimulationResult(**raw_result)

    except Exception as e:
        print("Simulation error:", repr(e))
        raise HTTPException(status_code=500, detail=f"Simulation crash: {e}")

    return sim_result


# ---------------------------
# /recommendations route
# ---------------------------

@app.post("/recommendations", response_model=RecommendationResponse)
def get_recommendations(
    payload: RetirementInput,
    token: str = Depends(oauth2_scheme),
    db: Session = Depends(get_db),
):
    """
    Return top-3 AI-powered recommendations based on the same payload
    used for /simulate (RetirementInput).

    - Requires Authorization: Bearer <token>
    - Returns { "recommendations": [ { id, title, description, impact_summary }, ... ] }
    """
    # 1) Validate token
    username = decode_access_token(token)
    if not username:
        raise HTTPException(status_code=401, detail="Invalid or expired token")

    try:
        # 2) Call rec_model to get raw dicts
        rec_dicts = recommend_for_payload(payload.dict(), top_k=3)

        # 3) Convert to Pydantic models for validation/serialization
        rec_objects = [Recommendation(**r) for r in rec_dicts]

    except Exception as e:
        print("Recommendation error:", repr(e))
        raise HTTPException(status_code=500, detail=f"Recommendation crash: {e}")

    return RecommendationResponse(recommendations=rec_objects)