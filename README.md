Carbon-Intensity Aware VM Placement in Cloud Data Centers

This project implements a Carbon-Intensity Aware Virtual Machine Placement (CIAVMP) algorithm using CloudSim Plus. The goal of the project is to reduce carbon emissions in cloud data centers by considering carbon intensity, power consumption, and CPU utilization during VM allocation.

Cloud data centers consume a large amount of electricity, and the carbon emissions depend on the type of energy used in different regions. Traditional VM allocation strategies focus only on resource utilization or energy consumption. This project introduces a carbon-aware approach that also considers the carbon intensity of electricity when placing virtual machines.

The system compares three VM allocation strategies:

First Fit VM Allocation

Energy-Aware VM Allocation

Carbon-Intensity Aware VM Allocation (CIAVMP)

The results show that CIAVMP reduces carbon emissions significantly while maintaining similar energy consumption and execution performance.

Technologies Used

Java
CloudSim Plus
Maven
Python (Matplotlib for result visualization)

System Architecture

The system consists of several components working together to simulate cloud scheduling.

User / Client
The user submits virtual machine requests and workloads to the system.

Global Datacenter Broker
The broker receives VM requests, selects the VM allocation policy, and coordinates scheduling across data centers.

Carbon Intensity Provider
The carbon intensity provider supplies region-wise carbon intensity values measured in gCO2/kWh. The RealTimeCarbonIntensityProvider simulates variations in carbon intensity across different regions.

VM Allocation Policies
Three allocation policies are implemented in the project:

FirstFitVmAllocationPolicy – assigns the VM to the first available host.
EnergyVmAllocationPolicy – selects the host that minimizes additional energy consumption.
CarbonVmAllocationPolicy – implements the CIAVMP algorithm and considers utilization, power consumption, and carbon intensity.

Cloud Datacenters
The simulation contains multiple regions with hosts that provide CPU, RAM, and bandwidth resources.

Results Logger
The ResultsLogger records energy consumption, carbon emissions, and makespan for each policy.

Visualization Module
A Python script generates graphs comparing the performance of different allocation policies.

Proposed Algorithm: CIAVMP

The Carbon-Intensity Aware VM Placement (CIAVMP) algorithm selects the most suitable host for a VM by minimizing a multi-objective score.

The score is calculated using three parameters:

CPU utilization
Power consumption
Regional carbon intensity

Score = alpha × CPU Utilization + beta × Power Consumption + gamma × Carbon Intensity

Where alpha, beta, and gamma are weighting factors used to balance the importance of each parameter.

The host with the lowest score is selected for VM allocation.

Project Structure

VM_Placement

src/main/java
CarbonSimulation.java
CarbonVmAllocationPolicy.java
EnergyVmAllocationPolicy.java
FirstFitVmAllocationPolicy.java
CarbonIntensityProvider.java
RealTimeCarbonIntensityProvider.java
ResultsLogger.java

plots_results.py
pom.xml
README.md

How to Run the Project

Step 1: Clone the Repository

git clone https://github.com/Vsd1208/VM_Placement.git

cd VM_Placement

Step 2: Build the Project

mvn clean install

Step 3: Run the Simulation

java -jar target/vm-placement-1.0.jar

The simulation will run and compare the three VM allocation policies.

Generating Graphs

Run the Python script to generate graphs for the experimental results.

python plots_results.py

This script generates comparison graphs for makespan, energy consumption, and carbon emissions.

Experimental Results

Makespan

All policies show similar execution time of approximately 14.4 seconds, indicating that CIAVMP introduces negligible scheduling overhead.

Energy Consumption

First Fit – approximately 0.042 kWh
Energy Aware – approximately 0.025 kWh
CIAVMP – approximately 0.025 kWh

Energy-Aware and CIAVMP reduce energy consumption by about 40 percent compared to First Fit.

Carbon Emissions

First Fit – approximately 0.0158 kg CO2
Energy Aware – approximately 0.0093 kg CO2
CIAVMP – approximately 0.0069 kg CO2

CIAVMP reduces carbon emissions by about 56 percent compared to First Fit and about 26 percent compared to the Energy-Aware policy.

Units Used

Energy Consumption – kWh
Carbon Emissions – kg CO2
Carbon Intensity – gCO2/kWh
Power – Watts (W)

These units follow internationally accepted standards used in cloud sustainability research.

Future Work

Future improvements for the project include integrating real-time carbon intensity data from APIs such as ElectricityMap, supporting dynamic VM migration based on carbon intensity changes, integrating the algorithm with Kubernetes schedulers, and adding SLA-aware carbon optimization.
