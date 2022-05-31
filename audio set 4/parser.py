import scipy.io.wavfile as wf
import numpy as np
RATE = 4000
# ... your code that puts an array of floats into data ...

data = np.array([])
with open("./FILTERED DATA 2 - Splited") as file:
    # with open("./RAW DATA 2 - Splited") as file:
    for d in file.readlines():
        data = np.append(data, [float(int(d) % 256)/256])
        data = np.append(data, [float(int(d)//256)/256])

wf.write('test.wav', RATE, data)
