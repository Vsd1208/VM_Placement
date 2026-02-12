import csv
from pathlib import Path

import matplotlib.pyplot as plt

SUMMARY_CSV = Path("results") / "evaluation_policy_summary.csv"


def read_summary(path: Path):
    rows = []
    with path.open("r", newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            rows.append(row)
    return rows


def to_policy_label(policy_name: str) -> str:
    if policy_name == "FIRST_FIT":
        return "First Fit"
    if policy_name == "ENERGY_AWARE":
        return "Energy Aware"
    return "CIAVMP"


def save_bar_with_error(
    labels,
    means,
    stds,
    ylabel,
    title,
    output_file,
    color,
):
    plt.figure(figsize=(8, 5))
    plt.bar(labels, means, yerr=stds, capsize=6, color=color)
    plt.ylabel(ylabel)
    plt.title(title)
    plt.tight_layout()
    plt.savefig(output_file, dpi=300)


if __name__ == "__main__":
    if not SUMMARY_CSV.exists():
        raise FileNotFoundError(
            f"{SUMMARY_CSV} not found. Run the Java simulation first."
        )

    data = read_summary(SUMMARY_CSV)
    labels = [to_policy_label(row["policy"]) for row in data]

    energy_mean = [float(row["energy_mean_kwh"]) for row in data]
    energy_std = [float(row["energy_std_kwh"]) for row in data]
    carbon_mean = [float(row["carbon_mean_kg"]) for row in data]
    carbon_std = [float(row["carbon_std_kg"]) for row in data]
    makespan_mean = [float(row["makespan_mean_s"]) for row in data]
    makespan_std = [float(row["makespan_std_s"]) for row in data]

    save_bar_with_error(
        labels,
        energy_mean,
        energy_std,
        "Energy (kWh)",
        "Energy Consumption Comparison",
        Path("results") / "energy_comparison.png",
        "#4E79A7",
    )

    save_bar_with_error(
        labels,
        carbon_mean,
        carbon_std,
        "Carbon (kg CO2)",
        "Carbon Emission Comparison",
        Path("results") / "carbon_comparison.png",
        "#F28E2B",
    )

    save_bar_with_error(
        labels,
        makespan_mean,
        makespan_std,
        "Makespan (s)",
        "Execution Time Comparison",
        Path("results") / "makespan_comparison.png",
        "#59A14F",
    )

    plt.show()
