import binascii


def int_to_byte_str(value):
    s = str(hex(value)).replace('0x', '')
    return s if len(s) == 2 else ('0'+s)


with open("./FILTERED DATA 4") as f, open('test.wav', 'wb') as fout:
    data = []
    for d in f.readlines():
        d = int(d)
        data.append(d % 256)
        data.append((d//256) % 256)

    # file_size = len(data)+44  # 27947
    file_size = 50000+44  # 27947
    sample_rate = 8000
    num_channels = 1
    bits_per_sample = 16
    byte_rate = (sample_rate * num_channels * bits_per_sample) // 8
    block_align = (num_channels * bits_per_sample) // 8
    data_size = len(data)

    print("file size: "+str(file_size))

    header1 = ["52", "49", "46", "46",  # RIFF

               # Chunk size = fileSize - 8
               #    int_to_byte_str((file_size-8) % 256),
               #    int_to_byte_str(((file_size-8)//256) % 256),
               #    int_to_byte_str(((file_size-8)//(256*256)) % 256),
               #    int_to_byte_str(((file_size-8)//(256*256*256)) % 256),

               "2C", "53", "07", "00",

               "57", "41", "56", "45", 	# WAVE

               "66", "6d", "74", "20",  # fmt
               "10", "00", "00", "00",  # subchunk 1 size

               "01", "00",  # audio format

               # num channels
               int_to_byte_str(num_channels), "00",

               # sample rate
               int_to_byte_str(sample_rate % 256),
               int_to_byte_str((sample_rate//256) % 256),
               int_to_byte_str((sample_rate//(256*256)) % 256),
               int_to_byte_str((sample_rate//(256*256*256)) % 256),

               # byte rate
               int_to_byte_str(byte_rate % 256),
               int_to_byte_str((byte_rate//256) % 256),
               int_to_byte_str((byte_rate//(256*256)) % 256),
               int_to_byte_str((byte_rate//(256*256*256)) % 256),

               # block align
               int_to_byte_str(block_align % 256),
               int_to_byte_str((block_align//256) % 256),

               # bits per sample
               int_to_byte_str(bits_per_sample % 256),
               int_to_byte_str((bits_per_sample//256) % 256),

               "64", "61", "74", "61",  # data
               # subchunk 2 size
               "00", "53", "07", "00",
               #    int_to_byte_str(data_size % 256),
               #    int_to_byte_str((data_size//256) % 256),
               #    int_to_byte_str((data_size//(256*256)) % 256),
               #    int_to_byte_str((data_size//(256*256*256)) % 256),
               ]

    for b in header1:
        fout.write(binascii.unhexlify(b))

    counter = 0
    temp = 0
    for b in data:
        fout.write(b.to_bytes(1, 'big'))
    # fout.write(binascii.unhexlify(b))
