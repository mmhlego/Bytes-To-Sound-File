with open("./StethoscopeSound.wav", 'rb') as f1, open("./test.wav", 'rb') as f2:
    for i in range(160300):
        b1 = f1.read(1)
        b2 = f2.read(1)

        if b1 != b2:
            print(f"${i} : ${b1} - ${b2}")
