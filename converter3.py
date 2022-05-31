import io
import wave

import numpy as np
import scipy.io.wavfile
import soundfile as sf
from scipy.io.wavfile import write

audio_file_path = 'test3.wav'

chunk = 1024

wf = wave.open(audio_file_path, 'rb')
audio_input = b''
d = wf.readframes(chunk)
while len(d) > 0:
    d = wf.readframes(chunk)
    audio_input = audio_input + d


def convert_bytearray_to_wav_ndarray(input_bytearray: bytes, sampling_rate=16000):
    bytes_wav = bytes()
    byte_io = io.BytesIO(bytes_wav)
    write(byte_io, sampling_rate, np.frombuffer(
        input_bytearray, dtype=np.int16))
    output_wav = byte_io.read()
    output, samplerate = sf.read(io.BytesIO(output_wav))
    return output


output = convert_bytearray_to_wav_ndarray(input_bytearray=audio_input)
