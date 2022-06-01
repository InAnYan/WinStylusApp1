#include <iostream>
#include <string>

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <winsock2.h>
#include <ws2tcpip.h>

#pragma comment (lib, "Ws2_32.lib")

using namespace std;

int main()
{
	cout << "WinStylusApp1 v0.1" << endl;
	cout << "Copyright (C) 2022 Ruslan Popov <ruslanpopov1512@gmail.com>" << endl;

	WSADATA wsa;
	SOCKET sock;

	if (WSAStartup(MAKEWORD(2, 2), &wsa) != 0)
	{
		cout << "ERROR: WSAStartup. Error Code: " + to_string(WSAGetLastError()) << endl;
		return 1;
	}

	struct addrinfo hints;
	ZeroMemory(&hints, sizeof(hints));
	hints.ai_family = AF_INET;
	hints.ai_socktype = SOCK_STREAM;
	hints.ai_protocol = IPPROTO_TCP;
	hints.ai_flags = AI_PASSIVE;

	struct addrinfo* result = NULL;
	
	if (getaddrinfo(NULL, "55555", &hints, &result) != 0) {
		cout << "ERROR: getaddrinfo. Error Code: " + to_string(WSAGetLastError()) << endl;
		return 1;
	}

	sockaddr_in* addr = (struct sockaddr_in*)result->ai_addr;
	char ipstr[INET_ADDRSTRLEN];
	inet_ntop(result->ai_family, addr, (PSTR)ipstr, sizeof(ipstr));
	cout << "Listening on " + string(ipstr) + ":55555" << endl;

	if ((sock = socket(result->ai_family, result->ai_socktype, result->ai_protocol)) == INVALID_SOCKET)
	{
		cout << "ERROR: socket. Error Code: " + to_string(WSAGetLastError()) << endl;
		return 1;
	}

	if (bind(sock, result->ai_addr, sizeof(*(result->ai_addr))) < 0)
	{
		cout << "ERROR: bind. Error Code: " + to_string(WSAGetLastError()) << endl;
		return 1;
	}

	freeaddrinfo(result);

	if (listen(sock, 1) == SOCKET_ERROR)
	{
		cout << "ERROR: listen. Error Code: " + to_string(WSAGetLastError()) << endl;
		return 1;
	}

	typedef struct
	{
		double x;
		double y;
		uint16_t s1;
	} status_t;

	union
	{
		status_t data;
		char raw[sizeof(status_t)];
	} status;

	status.data.x = 100;
	status.data.y = 100;
	status.data.s1 = 0;

	INPUT input;
	ZeroMemory(&input, sizeof(input));
	input.type = INPUT_MOUSE;
	input.mi.dwFlags = MOUSEEVENTF_LEFTUP;

	int screenWidth, screenHeight;
	screenWidth = GetSystemMetrics(SM_CXSCREEN);
	screenHeight = GetSystemMetrics(SM_CYSCREEN);
	status_t old;
	old.s1 = 0;
	old.x = 100;
	old.y = 100;

	int bytesRevieved;
	bool sendInput, hasMotion;

connect:
	SOCKET work;
	if ((work = accept(sock, NULL, NULL)) == SOCKET_ERROR)
	{
		cout << "ERROR: accept. Error Code: " + to_string(WSAGetLastError()) << endl;
		return 1;
	}
	cout << "Connected" << endl;

	while (true)
	{
		if ((bytesRevieved = recv(work, status.raw, sizeof(status_t), 0)) == SOCKET_ERROR)
		{
			cout << "ERROR: recv. Error Code: " + to_string(WSAGetLastError()) << endl;
			cout << "Connection lost. Reconnecting..." << endl;
			goto connect;
		}
		else if (bytesRevieved == 0)
		{
			cout << "Connection lost. Reconnecting..." << endl;
			goto connect;
		}

		if (status.data.s1 & 1) input.mi.dwFlags = MOUSEEVENTF_LEFTDOWN;
		else input.mi.dwFlags = MOUSEEVENTF_LEFTUP;
		if (status.data.s1 & 2) input.mi.dwFlags = MOUSEEVENTF_RIGHTDOWN;
		else input.mi.dwFlags = MOUSEEVENTF_RIGHTUP;

		if (old.s1 == status.data.s1) sendInput = false;
		else
		{
			sendInput = true;
			if (!(old.s1 & 1) && status.data.s1 & 1) input.mi.dwFlags = MOUSEEVENTF_LEFTDOWN;
			else if (old.s1 & 1 && !(status.data.s1 & 1)) input.mi.dwFlags = MOUSEEVENTF_LEFTUP;
			else if (!(old.s1 & 2) && status.data.s1 & 2) input.mi.dwFlags = MOUSEEVENTF_RIGHTDOWN;
			else if (old.s1 & 2 && !(status.data.s1 & 2)) input.mi.dwFlags = MOUSEEVENTF_RIGHTUP;
		}

		if (old.x != status.data.x || old.y != status.data.y) hasMotion = true;
		else hasMotion = false;

		old = status.data;

		if (hasMotion) SetCursorPos(status.data.x * screenWidth, status.data.y * screenHeight);
		if (sendInput) SendInput(1, &input, sizeof(input));
	}

	string temp;
	cout << "End of program. Press ENTER";
	cin >> temp;

	return 0;
}