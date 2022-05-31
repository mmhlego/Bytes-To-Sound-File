import soundfile as sf
with open('./file_example_WAV_1MG.wav', 'rb') as f:
    data, samplerate = sf.read(f)

    print(samplerate)
    print(data[0])
