import mmap
import struct
import time

print("Python started", flush=True)

FILE = "shared_memory.dat"

with open(FILE, "r+b") as f:
    mm = mmap.mmap(f.fileno(), 1024)

    while True:

        # read flag
        mm.seek(8)
        flag_bytes = mm.read(4)
        flag = struct.unpack(">i", flag_bytes)[0]

        # DEBUG (VERY IMPORTANT)
        if flag != 0:
            print("Flag:", flag, flush=True)

        if flag == 9:
            print("Python stopping...", flush=True)
            break

        if flag == 1:

            # read input
            mm.seek(0)
            number = struct.unpack(">i", mm.read(4))[0]

            print("Python received:", number, flush=True)

            # process
            result = number #+ 1

            # write result
            mm.seek(4)
            mm.write(struct.pack(">i", result))

            # set flag = 2
            mm.seek(8)
            mm.write(struct.pack(">i", 2))
            print("Flag:", flag, flush=True)
        # unlock

        time.sleep(0.001)