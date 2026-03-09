#Carbon-Intensity Aware VM Placement in Cloud Data Centers#

A Green Cloud Computing project that implements a Carbon-Intensity Aware Virtual Machine Placement (CIAVMP) algorithm using CloudSim Plus.
The project evaluates how VM scheduling decisions can reduce carbon emissions in cloud data centers by considering regional carbon intensity, power consumption, and CPU utilization during VM allocation.

##Project Overview##
Cloud data centers consume massive amounts of electricity, which contributes to global carbon emissions. Traditional VM allocation strategies optimize for resource utilization or energy efficiency, but they ignore the carbon intensity of electricity sources.
This project introduces CIAVMP, a carbon-aware VM placement strategy that schedules workloads to hosts located in lower-carbon regions, thereby reducing environmental impact.
The system compares three allocation strategies:
1.First Fit VM Allocation
2.Energy-Aware VM Allocation
3.Carbon-Intensity Aware VM Allocation (CIAVMP)
Simulation results show that CIAVMP significantly reduces carbon emissions without affecting performance.

##Technologies Used##
>Java
>CloudSim Plus
>Maven
>Python (Matplotlib for visualization)

##System Architecture##
The architecture consists of the following components:
1.User / Client
>Submits VM requests and workloads.
2.Global Datacenter Broker
>Receives VM requests
>Selects VM allocation policy
>Queries carbon intensity data
3.Carbon Intensity Provider
>Provides regional carbon intensity values
>Simulates real-time carbon variations.
4.VM Allocation Policies
>FirstFitVmAllocationPolicy
>EnergyVmAllocationPolicy
>CarbonVmAllocationPolicy (CIAVMP)
5.Cloud Datacenters
>Multiple regions containing physical hosts.
6.Results Logger
>Logs energy consumption, carbon emissions, and execution time.
7.Visualization Module
>Python script generates performance comparison graphs.
Python script generates performance comparison graphs.
