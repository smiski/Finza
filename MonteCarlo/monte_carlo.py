import json
import numpy as np
from datetime import datetime

def simulate_portfolio(individuals, joint=None, pre_ret_expenses=60000, post_ret_expenses=60000, returns=None, inflation=0.025):
    """
    Run one Monte Carlo simulation for all individuals together.
    """
    # Determine simulation horizon: until all individuals reach their planning horizon
    max_horizon = max(ind.get("planning_horizon", 90) - ind.get("current_age", 60) for ind in individuals) + 1
    
    # Initialize portfolio: combine all individual portfolios + joint if provided
    portfolio = {"cash": 0, "taxable": 0, "tax_deferred": 0, "tax_free": 0}
    for ind in individuals:
        for k, v in ind["portfolio"].items():
            portfolio[k] += v
    if joint:
        for k, v in joint["portfolio"].items():
            portfolio[k] += v
    
    # Precompute income and savings arrays with inflation applied
    income_array = np.zeros(max_horizon)
    savings_dict = {k: np.zeros(max_horizon) for k in portfolio}
    
    for ind in individuals:
        pre_income = ind.get("pre_retirement_income", 0)
        post_income = ind.get("post_retirement_income", 0)
        retirement_age = ind.get("retirement_age", 65)
        current_age = ind.get("current_age", 60)
        planning_horizon = ind.get("planning_horizon", 90)
        for year in range(max_horizon):
            age = current_age + year
            if age > planning_horizon:
                continue  # deceased, no income
            if age < retirement_age:
                income_array[year] += pre_income * ((1 + inflation) ** year)
            else:
                income_array[year] += post_income * ((1 + inflation) ** year)
        for k, val in ind["annual_savings"].items():
            for year in range(max_horizon):
                age = current_age + year
                if age > planning_horizon:
                    continue
                savings_dict[k][year] += val * ((1 + inflation) ** year)
    
    if joint:
        for k, val in joint["annual_savings"].items():
            for year in range(max_horizon):
                savings_dict[k][year] += val * ((1 + inflation) ** year)
    
    # Simulate year by year
    portfolio_history = []
    last_funded_year = 0
    ages_per_year = []
    last_funded_age = 0

    for year in range(max_horizon):
        # Apply market returns
        for k in portfolio:
            r_mean = returns[k]["mean"]
            r_vol = returns[k]["volatility"]
            random_return = np.random.normal(r_mean, r_vol)
            portfolio[k] *= (1 + random_return)

        # Add savings and income
        for k in portfolio:
            portfolio[k] += savings_dict[k][year]
        portfolio["cash"] += income_array[year]

        # Determine expenses
        min_ret_age = min(ind.get("retirement_age", 65) for ind in individuals)
        age_list = []
        for ind in individuals:
            age = ind.get("current_age", 60) + year
            if age > ind.get("planning_horizon", 90):
                age_list.append("XX")
            else:
                age_list.append(age)
        ages_per_year.append(age_list.copy())

        if all(a != "XX" and a < min_ret_age for a in age_list):
            expenses = pre_ret_expenses * ((1 + inflation) ** year)
        else:
            expenses = post_ret_expenses * ((1 + inflation) ** year)

        # Withdraw expenses: cash -> taxable -> tax_deferred -> tax_free
        remaining = expenses
        for bucket in ["cash", "taxable", "tax_deferred", "tax_free"]:
            withdrawal = min(portfolio[bucket], remaining)
            portfolio[bucket] -= withdrawal
            remaining -= withdrawal
            if remaining <= 0:
                break

        # Append portfolio for this year **after withdrawals**
        portfolio_history.append({k: round(v, 2) for k, v in portfolio.items()})

        # Update last_funded_age
        if remaining > 0:
            # Portfolio depleted this year
            last_funded_age = min(age for age in age_list if age != "XX")
            break
        else:
            # Portfolio still has funds
            last_funded_age = max(last_funded_age, max(age for age in age_list if age != "XX"))

    return portfolio_history, last_funded_age, ages_per_year


def run_simulation(json_file, num_simulations=None):
    """
    Run Monte Carlo simulation based on JSON input.
    """
    with open(json_file, 'r') as f:
        data = json.load(f)
    
    individuals = [ind for ind in data["individuals"] if ind.get("name") != "Joint"]
    joint = next((ind for ind in data["individuals"] if ind.get("name") == "Joint"), None)
    
    pre_ret_expenses = data.get("pre_retirement_expenses", 60000)
    post_ret_expenses = data.get("post_retirement_expenses", 60000)
    returns = data.get("returns")
    inflation = data.get("inflation", 0.025)
    
    if num_simulations is None:
        num_simulations = data.get("num_simulations", 1000)
    
    all_histories = []
    funds_last_ages = []
    ending_balances = []
    ages_history = []

    for _ in range(num_simulations):
        hist, last_years, ages_per_year = simulate_portfolio(
            individuals, joint, pre_ret_expenses, post_ret_expenses, returns, inflation
        )
        all_histories.append(hist)
        ending_balances.append(sum(hist[-1].values()))

        # Compute actual ages when portfolio runs out
        if joint:
            start_age = max(ind['current_age'] for ind in individuals)
            max_horizon = max(ind['planning_horizon'] for ind in individuals)
        else:
            start_age = individuals[0]['current_age']
            max_horizon = individuals[0]['planning_horizon']

        funds_last_ages.append(last_years)

        ages_history = ages_per_year

    average_funds_last_age = round(np.mean(funds_last_ages), 2)

    # Average portfolio per year
    max_years = max(len(sim) for sim in all_histories)
    avg_portfolio_per_year = []

    for year in range(max_years):
        year_total = 0
        count = 0
        for sim in all_histories:
            if year < len(sim):
                year_total += sum(sim[year].values())
                count += 1
        avg_portfolio_per_year.append(round(year_total / count, 2))

    # Success rate: fraction of simulations ending with funds > 0
    success_rate = round(sum([1 for b in ending_balances if b > 0]) / num_simulations * 100, 2)

    ending_balances_array = np.array(ending_balances)
    mean_ending = np.mean(ending_balances_array)
    std_ending = np.std(ending_balances_array)

    results = {
        "success_rate_percent": success_rate,
        "average_ending_balance": round(mean_ending, 2),
        "ending_balance_plus_1sd": round(mean_ending + std_ending, 2),
        "ending_balance_minus_1sd": round(mean_ending - std_ending, 2),
        "average_funds_last_age": average_funds_last_age,
        "average_portfolio_per_year": avg_portfolio_per_year,
        "ages_per_year": ages_history
    }
    
    return {"Combined": results}

if __name__ == "__main__":
    results = run_simulation("married_retirement_input.json", num_simulations=1000)
    
    current_year = datetime.now().year
    for name, res in results.items():
        print(f"--- {name} ---")
        print(f"Success Rate: {res['success_rate_percent']:.2f}%")
        print(f"Average Ending Balance: ${res['average_ending_balance']:,.2f}")
        print(f"Ending Balance + 1 SD: ${res['ending_balance_plus_1sd']:,.2f}")
        print(f"Ending Balance - 1 SD: ${res['ending_balance_minus_1sd']:,.2f}")
        print(f"Average Age Funds Last To: {res['average_funds_last_age']:.2f}")
        print()
        print("Average Portfolio Balance per Year:")
        # Printing aligned ages with average portfolio
        for i, val in enumerate(res["average_portfolio_per_year"]):
            year = current_year + i
            if i < len(res["ages_per_year"]):
                ages_list = [str(age) for age in res["ages_per_year"][i]]
            else:
                ages_list = ["XX"] * len(res["ages_per_year"][0])
            ages = "/".join(ages_list)
            print(f"{year} {ages}: ${val:,.2f}")