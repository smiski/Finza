"""
Simple PyTorch recommender for financial-planning suggestions.
- Reads JSON of the format you provided (hard-coded sample included).
- Uses a heuristic rule-based labeler to create synthetic training data.
- Trains a small feedforward network to predict top-3 recommendations.
"""

import json
import random
import numpy as np
import pandas as pd
import torch
import torch.nn as nn
from torch.utils.data import Dataset, DataLoader

# --------------------------
# 1) 100 recommendations 
# --------------------------
RECOMMENDATIONS = [
"Track every expense for at least one month.",
"Create a written monthly budget.",
"Separate fixed and variable expenses.",
"Set spending limits by category.",
"Use budgeting apps (e.g., YNAB, Mint, EveryDollar).",
"Review your budget monthly and adjust.",
"Automate bill payments to avoid late fees.",
"Prioritize needs over wants.",
"Set up direct deposit for income.",
"Review subscriptions and cancel unused ones.",
"Build a “zero-based” budget (every dollar has a purpose).",
"Use cash or debit for discretionary spending.",
"Track irregular or annual expenses (like insurance premiums).",
"Review and renegotiate bills (internet, phone, insurance).",
"Check your budget alignment with long-term goals.",
"Save at least 10–20% of your income.",
"Build an emergency fund of 3–6 months’ expenses.",
"Keep your emergency fund in a high-yield savings account.",
"Automate transfers to your savings account.",
"Avoid touching your emergency fund unless truly necessary.",
"Save windfalls (bonuses, tax refunds, gifts) instead of spending them.",
"Set short-term savings goals (vacations, car repairs).",
"Open separate savings accounts for different goals.",
"Refill your emergency fund after using it.",
"Review your savings rate yearly and increase it when possible.",
"List all your debts and interest rates.",
"Pay more than the minimum payment when possible.",
"Use the debt snowball (smallest balance first) or avalanche (highest rate first) method.",
"Refinance high-interest loans if feasible.",
"Avoid payday or title loans.",
"Consolidate debts cautiously—only if it reduces your total cost.",
"Stop using credit for discretionary purchases until debt is under control.",
"Create a timeline for becoming debt-free.",
"Celebrate milestones when you pay off a debt.",
"Don’t close old credit cards abruptly (it can lower credit score).",
"Check your credit reports annually at AnnualCreditReport.com.",
"Dispute errors on your credit report.",
"Keep credit utilization below 30%.",
"Pay all bills on time, every time.",
"Avoid applying for too many new accounts at once.",
"Keep old accounts open to lengthen credit history.",
"Consider a secured card if you’re building credit.",
"Set up reminders or autopay for credit card bills.",
"Understand your credit score factors.",
"Aim for a credit score above 750 for best rates.",
"Start investing early — time matters more than timing.",
"Contribute to your employer’s 401(k), especially to get the full match.",
"Open an IRA (Roth or Traditional) if eligible.",
"Diversify your investments (stocks, bonds, funds).",
"Avoid trying to “time the market.”",
"Reinvest dividends instead of cashing them out.",
"Keep investing consistent, even during market downturns.",
"Review and rebalance your portfolio annually.",
"Learn the basics of index funds and ETFs.",
"Keep investment costs and fees low.",
"Avoid emotional decisions when markets fluctuate.",
"Set clear, measurable investment goals (e.g., $500k by age 60).",
"Understand your risk tolerance before investing.",
"Don’t invest in things you don’t understand.",
"Use tax-advantaged accounts before taxable ones.",
"Estimate your retirement expenses.",
"Use online retirement calculators for planning.",
"Aim to replace 70–80% of your pre-retirement income.",
"Take advantage of employer matching programs.",
"Increase contributions with every raise.",
"Avoid early withdrawals from retirement accounts.",
"Understand required minimum distributions (RMDs).",
"Diversify across tax types (tax-deferred, taxable, tax-free).",
"Consider delaying Social Security for higher benefits.",
"Revisit your retirement plan every year.",
"Maintain adequate health insurance coverage.",
"Have term life insurance if others depend on your income.",
"Consider disability insurance for income protection.",
"Carry renters or homeowners insurance.",
"Review auto insurance annually for coverage and cost.",
"Consider umbrella insurance for additional liability protection.",
"Avoid unnecessary insurance products (e.g., extended warranties).",
"Keep beneficiary designations up to date.",
"Understand your deductibles and policy limits.",
"Shop around for better insurance rates periodically.",
"File your taxes on time every year.",
"Understand your tax bracket and deductions.",
"Contribute to tax-advantaged accounts (401k, HSA, IRA).",
"Keep digital and paper copies of key financial documents.",
"Adjust your tax withholding if you consistently owe or get large refunds.",
"Track charitable donations for deductions.",
"Understand capital gains and how they affect your taxes.",
"Hire a tax professional for complex situations.",
"Plan ahead for estimated tax payments if self-employed.",
"Review your tax plan each year, especially after major life changes.",
"Read at least one personal finance book per year.",
"Follow reliable finance blogs or podcasts.",
"Avoid comparing your finances to others.",
"Set SMART financial goals (Specific, Measurable, Achievable, Relevant, Time-bound).",
"Review your goals quarterly.",
"Discuss finances openly with your partner.",
"Keep learning about investing, credit, and taxes.",
"Teach kids or younger relatives basic money skills.",
"Schedule a yearly “financial checkup.”",
"Remember — consistency beats perfection."
]

# --------------------------
# sample JSON
# --------------------------
sample_json_text = r'''
{
  "individuals": [
    {
      "name": "Bill",
      "current_age": 60,
      "retirement_age": 67,
      "planning_horizon": 90,
      "pre_retirement_income": 80000,
      "post_retirement_income": 30000,
      "portfolio": {
        "cash": 5000,
        "taxable": 40000,
        "tax_deferred": 100000,
        "tax_free": 10000
      },
      "annual_savings": {
        "cash": 0,
        "taxable": 2000,
        "tax_deferred": 10000,
        "tax_free": 0
      }
    },
    {
      "name": "Sarah",
      "current_age": 58,
      "retirement_age": 65,
      "planning_horizon": 90,
      "pre_retirement_income": 60000,
      "post_retirement_income": 20000,
      "portfolio": {
        "cash": 500,
        "taxable": 0,
        "tax_deferred": 60000,
        "tax_free": 6000
      },
      "annual_savings": {
        "cash": 0,
        "taxable": 0,
        "tax_deferred": 5000,
        "tax_free": 500
      }
    },
    {
      "name": "Joint",
      "portfolio": {
        "cash": 10000,
        "taxable": 0,
        "tax_deferred": 0,
        "tax_free": 0
      },
      "annual_savings": {
        "cash": 1000,
        "taxable": 0,
        "tax_deferred": 0,
        "tax_free": 0
      }
    }
  ],
  "pre_retirement_expenses": 100000,
  "post_retirement_expenses": 100000,
  "returns": {
    "cash": {"mean": 0.01, "volatility": 0.005},
    "taxable": {"mean": 0.04, "volatility": 0.05},
    "tax_deferred": {"mean": 0.06, "volatility": 0.1},
    "tax_free": {"mean": 0.08, "volatility": 0.14}
  },
  "inflation": 0.025,
  "num_simulations": 10000
}
'''
data = json.loads(sample_json_text)

# --------------------------
# feature extraction
# --------------------------
def extract_features_for_individual(ind, global_data):
    p = ind.get("portfolio", {})
    a = ind.get("annual_savings", {})
    current_age = ind.get("current_age", 0) or 0
    retirement_age = ind.get("retirement_age", current_age)
    years_to_retire = max(0, retirement_age - current_age)
    planning_horizon = ind.get("planning_horizon", 90) - current_age if ind.get("planning_horizon") else 30
    if planning_horizon < 0: planning_horizon = 30
    total_portfolio = sum([p.get(k,0) for k in ["cash","taxable","tax_deferred","tax_free"]])
    cash_ratio = p.get("cash",0) / (total_portfolio + 1e-9)
    taxable_ratio = p.get("taxable",0) / (total_portfolio + 1e-9)
    tax_deferred_ratio = p.get("tax_deferred",0) / (total_portfolio + 1e-9)
    tax_free_ratio = p.get("tax_free",0) / (total_portfolio + 1e-9)
    annual_savings_total = sum([a.get(k,0) for k in ["cash","taxable","tax_deferred","tax_free"]])
    pre_inc = ind.get("pre_retirement_income", global_data.get("pre_retirement_expenses", 0))
    savings_rate_est = annual_savings_total / (pre_inc + 1e-9)
    near_retirement = 1.0 if years_to_retire <= 7 else 0.0
    low_cash_buffer = 1.0 if p.get("cash",0) < 0.5 * (pre_inc/12) else 0.0
    low_emergency = 1.0 if p.get("cash",0) <  pre_inc * 0.25 else 0.0
    return np.array([
        current_age/100.0,
        retirement_age/100.0,
        years_to_retire/100.0,
        planning_horizon/100.0,
        total_portfolio/100000.0,
        cash_ratio, taxable_ratio, tax_deferred_ratio, tax_free_ratio,
        annual_savings_total/10000.0,
        savings_rate_est,
        near_retirement,
        low_cash_buffer,
        low_emergency
    ], dtype=float)

# --------------------------
# rule-based labeler (heuristics) -> top 3 indices
# --------------------------
def heuristic_top3_recs(ind, global_data):
    scores = np.zeros(len(RECOMMENDATIONS), dtype=float)
    p = ind.get("portfolio", {})
    a = ind.get("annual_savings", {})
    current_age = ind.get("current_age", 0) or 0
    retirement_age = ind.get("retirement_age", current_age)
    years_to_retire = max(0, retirement_age - current_age)
    total_port = sum([p.get(k,0) for k in ["cash","taxable","tax_deferred","tax_free"]])
    pre_inc = ind.get("pre_retirement_income", global_data.get("pre_retirement_expenses", 0))
    months_cash = (p.get("cash",0)) / (pre_inc/12 + 1e-9)
    if months_cash < 3:
        scores[16] += 5.0   # emergency fund (index 16)
        scores[17] += 1.5
        scores[18] += 1.0
    if years_to_retire <= 7:
        scores[47] += 2.5  # contribute to 401k (index 47)
        scores[64] += 2.0  # increase contributions (index 64)
        scores[66] += 1.5  # diversify tax types (index 66)
        scores[68] += 1.0  # consider delaying Social Security (index 68)
        scores[60] += 1.0  # estimate retirement expenses (index 60)
    td = p.get("tax_deferred",0)
    if total_port > 0 and td > 0.6 * total_port:
        scores[65] += 2.0  # avoid early withdrawals (index 65)
        scores[66] += 2.0  # understand RMDs (index 66)
        scores[82] += 1.0
    annual_savings_total = sum([a.get(k,0) for k in ["cash","taxable","tax_deferred","tax_free"]])
    if annual_savings_total / (pre_inc + 1e-9) < 0.1:
        scores[15] += 3.0  # save 10-20%
        scores[18] += 1.0
        scores[39] += 1.0
    if total_port < pre_inc * 0.5:
        scores[0] += 1.0
        scores[1] += 1.0
        scores[24] += 1.0
    if p.get("taxable",0) < 0.1 * (total_port + 1e-9):
        scores[48] += 1.2
        scores[59] += 0.8
    scores[99] += 0.5
    scores[90] += 0.5
    top3 = list(np.argsort(-scores)[:3])
    return top3

# --------------------------
# synthetic dataset creation (perturb base individuals)
# --------------------------
def make_random_individual(base_ind, global_data):
    ind = json.loads(json.dumps(base_ind))
    for key in ["cash","taxable","tax_deferred","tax_free"]:
        if "portfolio" in ind and key in ind["portfolio"]:
            change = 1 + random.uniform(-0.5, 0.5)
            ind["portfolio"][key] = max(0.0, ind["portfolio"][key] * change)
    if "annual_savings" in ind:
        for key in ["cash","taxable","tax_deferred","tax_free"]:
            if key in ind["annual_savings"]:
                change = 1 + random.uniform(-0.5, 0.5)
                ind["annual_savings"][key] = max(0.0, ind["annual_savings"][key] * change)
    if "current_age" in ind:
        ind["current_age"] = max(18, ind["current_age"] + random.randint(-3,3))
    if "retirement_age" in ind and "current_age" in ind:
        ind["retirement_age"] = max(ind["current_age"], ind["retirement_age"] + random.randint(-2,2))
    return ind

# build dataset
base_templates = data["individuals"]
X_list = []
Y_list = []
num_samples = 3000
for _ in range(num_samples):
    base = random.choice(base_templates)
    ind = make_random_individual(base, data)
    ind["pre_retirement_income"] = ind.get("pre_retirement_income", data.get("pre_retirement_expenses"))
    feat = extract_features_for_individual(ind, data)
    labels = heuristic_top3_recs(ind, data)
    target = np.zeros(len(RECOMMENDATIONS), dtype=float)
    target[labels] = 1.0
    X_list.append(feat)
    Y_list.append(target)

X = np.vstack(X_list).astype(np.float32)
Y = np.vstack(Y_list).astype(np.float32)

# --------------------------
# PyTorch dataset / model
# --------------------------
class RecDataset(Dataset):
    def __init__(self, X, Y):
        self.X = torch.from_numpy(X)
        self.Y = torch.from_numpy(Y)
    def __len__(self):
        return self.X.shape[0]
    def __getitem__(self, idx):
        return self.X[idx], self.Y[idx]

ds = RecDataset(X, Y)
loader = DataLoader(ds, batch_size=128, shuffle=True)

class SimpleRecModel(nn.Module):
    def __init__(self, in_dim, out_dim):
        super().__init__()
        self.net = nn.Sequential(
            nn.Linear(in_dim, 128),
            nn.ReLU(),
            nn.Linear(128, 128),
            nn.ReLU(),
            nn.Linear(128, out_dim)
        )
    def forward(self, x):
        return self.net(x)

device = torch.device("cpu")
model = SimpleRecModel(X.shape[1], Y.shape[1]).to(device)
optimizer = torch.optim.Adam(model.parameters(), lr=0.001)
criterion = nn.BCEWithLogitsLoss()

# train
model.train()
for epoch in range(8):
    epoch_loss = 0.0
    for xb, yb in loader:
        xb = xb.to(device)
        yb = yb.to(device)
        pred = model(xb)
        loss = criterion(pred, yb)
        optimizer.zero_grad()
        loss.backward()
        optimizer.step()
        epoch_loss += loss.item() * xb.size(0)
    epoch_loss /= len(ds)
    print(f"Epoch {epoch+1}/8 loss: {epoch_loss:.4f}")

# --------------------------
# predict top-3 for provided JSON
# --------------------------
model.eval()
results = []
with torch.no_grad():
    for ind in data["individuals"]:
        ind_local = dict(ind)
        ind_local["pre_retirement_income"] = ind_local.get("pre_retirement_income", data.get("pre_retirement_expenses"))
        feat = extract_features_for_individual(ind_local, data)
        x = torch.from_numpy(feat.astype(np.float32)).unsqueeze(0).to(device)
        logits = model(x).cpu().numpy().flatten()
        probs = 1 / (1 + np.exp(-logits))
        top3 = list(np.argsort(-probs)[:3])
        results.append({"name": ind.get("name",""), "top3": top3, "text": [RECOMMENDATIONS[i] for i in top3], "scores": [float(probs[i]) for i in top3]})

# pretty print
for r in results:
    print("----")
    print("Name:", r["name"])
    for i, (t, s) in enumerate(zip(r["text"], r["scores"]), start=1):
        print(f"  {i}. {t} (score {s:.3f})")