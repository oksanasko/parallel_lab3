import socket

HOST = "127.0.0.1"
PORT = 5000

print("Python server started")

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.bind((HOST, PORT))
    s.listen(1)

    conn, addr = s.accept()
    with conn:
        print("Connected by", addr)

        while True:
            data = conn.recv(4)
            if not data:
                break

            number = int.from_bytes(data, byteorder='big')
            print("Python received:", number)

            result = number #+ 1

            conn.sendall(result.to_bytes(4, byteorder='big'))