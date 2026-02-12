import matplotlib.pyplot as plt

methods = ['First Fit', 'Energy Aware', 'CIAVMP']
energy = [10.2, 8.6, 7.8]
carbon = [6.1, 5.0, 3.5]

plt.figure()
plt.bar(methods, energy)
plt.ylabel("Energy (kWh)")
plt.title("Energy Consumption Comparison")
plt.savefig("energy.png")

plt.figure()
plt.bar(methods, carbon)
plt.ylabel("Carbon (kg CO2)")
plt.title("Carbon Emission Comparison")
plt.savefig("carbon.png")

plt.show()
