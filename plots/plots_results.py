import csv
from pathlib import Path

import matplotlib.pyplot as plt
from matplotlib.ticker import FuncFormatter

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
    decimals=2,
):
    plt.figure(figsize=(8, 5))
    bars = plt.bar(labels, means, yerr=stds, capsize=6, color=color)
    plt.ylabel(ylabel)
    plt.title(title)
    set_zoomed_y_limits(means, stds)
    plt.gca().yaxis.set_major_formatter(
        FuncFormatter(lambda y, _: f"{y:.{decimals}f}")
    )
    add_value_labels(bars, means, decimals)
    plt.grid(axis="y", linestyle="--", alpha=0.25)
    plt.tight_layout()
    plt.savefig(output_file, dpi=300)


def set_zoomed_y_limits(means, stds):
    lower_candidates = [m - s for m, s in zip(means, stds)]
    upper_candidates = [m + s for m, s in zip(means, stds)]
    y_min = min(lower_candidates)
    y_max = max(upper_candidates)

    if y_max <= y_min:
        y_min *= 0.98
        y_max *= 1.02

    span = y_max - y_min
    padding = max(span * 0.15, abs(y_max) * 0.03, 1e-6)
    plt.ylim(y_min - padding, y_max + padding)


def add_value_labels(bars, values, decimals):
    for bar, value in zip(bars, values):
        x = bar.get_x() + bar.get_width() / 2
        y = bar.get_height()
        plt.text(
            x,
            y,
            f"{value:.{decimals}f}",
            ha="center",
            va="bottom",
            fontsize=9,
        )


if __name__ == "__main__":
    if not SUMMARY_CSV.exists():
        raise FileNotFoundError(
            f"{SUMMARY_CSV} not found. Run the Java simulation first."
        )

    data = read_summary(SUMMARY_CSV)
    labels = [to_policy_label(row["policy"]) for row in data]

    energy_mean = [float(row["energy_mean_kwh"]) for row in data]      # kWh
    energy_std = [float(row["energy_std_kwh"]) for row in data]        # kWh
    carbon_mean = [float(row["carbon_mean_kg"]) for row in data]       # kg CO2e
    carbon_std = [float(row["carbon_std_kg"]) for row in data]         # kg CO2e
    makespan_mean = [float(row["makespan_mean_s"]) for row in data]
    makespan_std = [float(row["makespan_std_s"]) for row in data]

    save_bar_with_error(
        labels,
        energy_mean,
        energy_std,
        "Energy Consumption (kWh)",
        "Energy Consumption Comparison (Zoomed Scale)",
        Path("results") / "energy_comparison.png",
        "#4E79A7",
        decimals=6,
    )

    save_bar_with_error(
        labels,
        carbon_mean,
        carbon_std,
        "Carbon Emission (kg CO2e)",
        "Carbon Emission Comparison (Zoomed Scale)",
        Path("results") / "carbon_comparison.png",
        "#F28E2B",
        decimals=6,
    )

    save_bar_with_error(
        labels,
        makespan_mean,
        makespan_std,
        "Makespan (s)",
        "Execution Time Comparison (Zoomed Scale)",
        Path("results") / "makespan_comparison.png",
        "#59A14F",
        decimals=6,
    )

    plt.show()
