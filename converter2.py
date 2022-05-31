import io
from pydub import AudioSegment

current_data = []

with open('./audio set 3 - salam 2/raw data.txt') as f, open("RAW.raw", 'wb') as fout:
    data = f.readline().split('-')

    counter = 0
    temp = 0
    for b in data:
        temp = 256*temp + int(b, base=16)
        # current_data.append((int(b, base=16)).to_bytes(1, 'big'))
        fout.write(max((int(b, base=16)-128, 0)).to_bytes(1, 'big'))

        if counter == 0:
            # fout.write(((temp//(64*64*64)) % 64).to_bytes(1, 'big'))
            # fout.write(((temp//(64*64)) % 64).to_bytes(1, 'big'))
            # fout.write(((temp//64) % 64).to_bytes(1, 'big'))
            # fout.write((temp % 64).to_bytes(1, 'big'))
            temp = 0

        counter = (counter+1) % 3


with open("RAW.raw", 'rb') as fout:
    s = io.BytesIO(fout.read())
    audio = AudioSegment.from_raw(
        s, sample_width=1, frame_rate=4000, channels=1).export("test2.wav", format='wav')
