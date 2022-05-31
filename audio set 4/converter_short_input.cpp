#include <bits/stdc++.h>
#include <cstddef>
#include <bitset>
using namespace std;

#define HEADER_SIZE 44

int main(int argc, char* argv[]) {
	ios::sync_with_stdio(false); cin.tie(0); cout.tie(0);

	// int subchunk2size = 9472;
	int subchunk2size = 43520;
	int subchunk1size = 16;
	int chunksize = subchunk2size + 36;

	int audioFormat = 1;
	int numChannels = 1;

	int bitsPerSample = 16;

	int sampleRate = 4000;
	int byteRate = sampleRate * numChannels * bitsPerSample / 8;
	int blockAlign = numChannels * bitsPerSample / 8;

	if (argc < 2) {
		cout << "inputs: [output file]";
		return 1;
	}
	FILE* output = fopen(argv[1], "w");
	if (output == NULL) {
		cout << "Could not open output file";
		return 1;
	}

	uint8_t header[HEADER_SIZE];

	// "RIFF"
	header[0] = uint8_t(0x52);
	header[1] = uint8_t(0x49);
	header[2] = uint8_t(0x46);
	header[3] = uint8_t(0x46);

	// chunk size
	header[4] = uint8_t(chunksize % 256);
	header[5] = uint8_t((chunksize >> 8) % 256);
	header[6] = uint8_t((chunksize >> 16) % 256);
	header[7] = uint8_t((chunksize >> 24) % 256);

	// "WAVE"
	header[8] = uint8_t(0x57);
	header[9] = uint8_t(0x41);
	header[10] = uint8_t(0x56);
	header[11] = uint8_t(0x45);

	// "fmt "
	header[12] = uint8_t(0x66);
	header[13] = uint8_t(0x6d);
	header[14] = uint8_t(0x74);
	header[15] = uint8_t(0x20);

	// chunk size
	header[16] = uint8_t(subchunk1size % 256);
	header[17] = uint8_t((subchunk1size >> 8) % 256);
	header[18] = uint8_t((subchunk1size >> 16) % 256);
	header[19] = uint8_t((subchunk1size >> 24) % 256);

	// chunk audio format
	header[20] = uint8_t(audioFormat % 256);
	header[21] = uint8_t((audioFormat >> 8) % 256);

	// num channels
	header[22] = uint8_t(numChannels % 256);
	header[23] = uint8_t((numChannels >> 8) % 256);

	// sample rate
	header[24] = uint8_t(sampleRate % 256);
	header[25] = uint8_t((sampleRate >> 8) % 256);
	header[26] = uint8_t((sampleRate >> 16) % 256);
	header[27] = uint8_t((sampleRate >> 24) % 256);

	// byte rate
	header[28] = uint8_t(byteRate % 256);
	header[29] = uint8_t((byteRate >> 8) % 256);
	header[30] = uint8_t((byteRate >> 16) % 256);
	header[31] = uint8_t((byteRate >> 24) % 256);

	// block align
	header[32] = uint8_t(blockAlign % 256);
	header[33] = uint8_t((blockAlign >> 8) % 256);

	// bits per sample
	header[34] = uint8_t(bitsPerSample % 256);
	header[35] = uint8_t((bitsPerSample >> 8) % 256);

	// "data"
	header[36] = uint8_t(0x64);
	header[37] = uint8_t(0x61);
	header[38] = uint8_t(0x74);
	header[39] = uint8_t(0x61);

	// subchunk2 size
	header[40] = uint8_t((subchunk2size) % 256);
	header[41] = uint8_t((subchunk2size >> 8) % 256);
	header[42] = uint8_t((subchunk2size >> 16) % 256);
	header[43] = uint8_t((subchunk2size >> 24) % 256);

	fwrite(header, sizeof(uint8_t), HEADER_SIZE, output);

	for (int i = 0;i < subchunk2size;i++) {
		short inputShort;
		cin >> inputShort;

		cout << "From: " << inputShort << endl;
		// inputShort += 1 << 7;
		// cout << "To: " << inputShort << endl;

		uint8_t d0 = inputShort % 256;
		uint8_t d1 = (inputShort / 256) % 256;
		// uint8_t d2 = (inputShort >> 16) % 256;
		// uint8_t d3 = (inputShort >> 24) % 256;

		fwrite(&d0, sizeof(uint8_t), 1, output);
		fwrite(&d1, sizeof(uint8_t), 1, output);
		// fwrite(&d2, sizeof(uint8_t), 1, output);
		// fwrite(&d3, sizeof(uint8_t), 1, output);
	}
}