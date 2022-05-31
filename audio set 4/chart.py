import matplotlib.pyplot as plt

Wave = []
Values = []

with open("./RAW DATA 5 - Splitted") as file:
    generation = 1
    for data in file.readlines():
        # Wave.append(int(data))
        Wave.append(int(data))
        Values.append(generation)
        generation += 1

plt.plot(Values, Wave, color='red', marker='')
plt.show()
