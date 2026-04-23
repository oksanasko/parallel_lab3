import sys

print("Python worker started", file=sys.stderr)

for line in sys.stdin:
    line = line.strip()

    if not line:
        continue

    try:
        number = int(line)

        # log
        print(f"(not error)Received: {number}", file=sys.stderr)

        # real response (ONLY stdout)
        print(number, flush=True)

    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)