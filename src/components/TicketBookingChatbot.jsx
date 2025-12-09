import React, { useState, useRef, useEffect } from "react";
import { GoogleOAuthProvider } from "@react-oauth/google";
import NavBar from "./NavBar";
import ChatMessages from "./Chat/ChatMessages";
import ChatInput from "./Chat/ChatInput";
import DatePickerModal from "./Chat/DatePickerModal";
import SeatPickerModal from "./Chat/SeatPickerModal";
import TypingIndicator from "./Chat/TypingIndicator";
import RecordRTC from 'recordrtc';

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID;
const API_URL = 'https://moviebookingchatbot.development.catalystappsail.in';

const TicketBookingChatbot = () => {
    const [messages, setMessages] = useState([
        {
            sender: "Bot",
            text: "Hello! I'm your Movie Ticket Assistant. How can I help you today?",
            avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=MovieBot"
        }
    ]);
    const [input, setInput] = useState("");
    const [loading, setLoading] = useState(false);
    const [isRecording, setIsRecording] = useState(false);
    const [selectedDate, setSelectedDate] = useState(null);
    const [showDatePicker, setShowDatePicker] = useState(false);
    const [showSeatPicker, setShowSeatPicker] = useState(false);
    const [seatLayout, setSeatLayout] = useState([]);
    const [selectedSeats, setSelectedSeats] = useState([]);
    const [maxSeats, setMaxSeats] = useState(1);
    const [sessionId, setSessionId] = useState(() =>
        `session_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
    );
    const [user, setUser] = useState(null);

    const inputRef = useRef(null);
    const messagesEndRef = useRef(null);
    const recorderRef = useRef(null);

    // Auto-scroll to latest message
    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    };

    useEffect(() => {
        scrollToBottom();
    }, [messages, loading]);

    useEffect(() => {
        inputRef.current?.focus();
    }, [messages]);

    // Check for existing user on component mount
    useEffect(() => {
        const storedUser = localStorage.getItem("movieBotUser");
        if (storedUser) {
            try {
                const userData = JSON.parse(storedUser);
                setUser(userData);
                addBotMessage(`Welcome back, ${userData.name}! How can I help you today?`);
            } catch (e) {
                localStorage.removeItem("movieBotUser");
            }
        }
    }, []);

    // Handle Google login success
    const handleLoginSuccess = async (idToken, decodedToken) => {
        try {
            // Send token to your backend for verification
            const response = await fetch(`${API_URL}/api/auth/google`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({ idToken }),
            });

            if (!response.ok) {
                throw new Error("Login failed");
            }

            const userData = await response.json();

            // Store user data
            const userInfo = {
                ...userData,
                name: decodedToken.name,
                email: decodedToken.email,
                picture: decodedToken.picture,
                googleId: decodedToken.sub,
            };

            setUser(userInfo);
            localStorage.setItem("movieBotUser", JSON.stringify(userInfo));

            addBotMessage(`Welcome, ${userInfo.name}! How can I help you today?`);

            // Update session with user ID
            if (userData.userId) {
                setSessionId(`user_${userData.userId}_${Date.now()}`);
            }

        } catch (error) {
            console.error("Google login error:", error);
            addBotMessage("Login failed. Please try again.");
        }
    };

    // Handle logout
    const handleLogout = () => {
        setUser(null);
        localStorage.removeItem("movieBotUser");
        setSessionId(`session_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`);
        addBotMessage("You've been logged out successfully.");
    };

    const addUserMessage = (text) => {
        setMessages(prev => [
            ...prev,
            {
                sender: "You",
                text,
                avatar: user?.picture || "https://api.dicebear.com/7.x/avataaars/svg?seed=User"
            }
        ]);
    };

    const addBotMessage = (text) => {
        setMessages(prev => [
            ...prev,
            {
                sender: "Bot",
                text,
                avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=MovieBot"
            }
        ]);
    };

    const handleSendMessage = async (e) => {
        e.preventDefault();
        const trimmed = input.trim();
        if (!trimmed || loading) return;

        addUserMessage(trimmed);
        setInput("");
        await sendToBackend(trimmed, false);
    };

    const handleDateConfirm = () => {
        if (!selectedDate) return;

        const formattedDate = "On " + selectedDate.toISOString().split('T')[0];
        setShowDatePicker(false);

        addUserMessage(formattedDate);
        sendToBackend(formattedDate, false);
        setSelectedDate(null);
    };

    const handleDateChange = (date) => {
        setSelectedDate(new Date(date));
    };

    const sendToBackend = async (inputData, isAudio = false) => {
        setLoading(true);

        try {
            const formData = new FormData();
            if (isAudio) {
                formData.append('file', inputData, 'audio.wav');
            } else {
                formData.append('query', inputData);
            }
            formData.append('sessionId', sessionId);

            // Add user info if logged in
            if (user?.email) {
                formData.append('userEmail', user.email);
            }
            if (user?.googleId) {
                formData.append('userId', user.googleId);
            }

            // Make sure API_URL is correct
            const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:9000';

            const response = await fetch(`${API_URL}/chat/webhook`, {
                method: 'POST',
                body: formData,
                // Important: Don't set Content-Type header manually for FormData
                // Let browser set it automatically with boundary
                headers: {
                    'Accept': 'application/json',
                },
                credentials: 'include',
                mode: 'cors'
            });

            if (!response.ok) {
                const errorText = await response.text();
                console.error('Server error response:', errorText);
                throw new Error(`HTTP error! status: ${response.status}, message: ${errorText}`);
            }

            const data = await response.json();

            // Handle response
            if (data.messages && Array.isArray(data.messages)) {
                // Check if first message is JSON for seat selection
                if (data.messages.length > 0) {
                    try {
                        const firstMessage = data.messages[0];
                        const parsedData = JSON.parse(firstMessage);

                        if (parsedData.showSeatSelection) {
                            const availableSeats = parsedData.availableSeats || [];
                            setSeatLayout(generateSeatLayout(availableSeats));
                            setSelectedSeats([]);
                            setMaxSeats(parsedData.maxSeats || 1);
                            setShowSeatPicker(true);

                            // Show other messages
                            if (data.messages.length > 1) {
                                for (let i = 1; i < data.messages.length; i++) {
                                    setTimeout(() => {
                                        addBotMessage(data.messages[i]);
                                    }, i * 1000);
                                }
                            }
                            return;
                        }
                    } catch (e) {
                        // Not JSON, continue with normal messages
                    }
                }

                // Show all messages
                data.messages.forEach((message, index) => {
                    setTimeout(() => {
                        addBotMessage(message);
                    }, index * 1000);
                });
            }
        } catch (error) {
            console.error('Error details:', error);
            console.error('Error stack:', error.stack);
            addBotMessage("An error occurred. Please try again. Error: " + error.message);
        } finally {
            setLoading(false);
        }
    };

    const generateSeatLayout = (availableSeats = []) => {
        const rows = 8;
        const seatsPerRow = 10;
        const layout = [];

        for (let row = 0; row < rows; row++) {
            const rowLetter = String.fromCharCode(65 + row);
            const rowSeats = [];

            for (let seatNum = 1; seatNum <= seatsPerRow; seatNum++) {
                const seatId = `${rowLetter}${seatNum}`;
                const isAvailable = availableSeats.includes(seatId);

                rowSeats.push({
                    id: seatId,
                    number: seatId,
                    available: isAvailable,
                    booked: !isAvailable,
                    selected: false
                });
            }
            layout.push(rowSeats);
        }
        return layout;
    };

    const toggleSeatSelection = (seatId) => {
        setSeatLayout(prevLayout => {
            return prevLayout.map(row => {
                return row.map(seat => {
                    if (seat.id === seatId && seat.available) {
                        if (seat.selected) {
                            return { ...seat, selected: false };
                        } else if (selectedSeats.length < maxSeats) {
                            return { ...seat, selected: true };
                        }
                    }
                    return seat;
                });
            });
        });

        setSelectedSeats(prev => {
            if (prev.includes(seatId)) {
                return prev.filter(id => id !== seatId);
            } else if (prev.length < maxSeats) {
                return [...prev, seatId];
            }
            return prev;
        });
    };

    const handleSeatConfirm = async () => {
        const requiredSeats = maxSeats;

        if (selectedSeats.length !== requiredSeats) {
            addBotMessage(`Please select exactly ${requiredSeats} seat${requiredSeats > 1 ? 's' : ''}.`);
            return;
        }

        setLoading(true);
        setShowSeatPicker(false);

        addUserMessage(`Selected seats: ${selectedSeats.join(', ')}`);

        try {
            const formData = new FormData();
            formData.append('selected_seats', selectedSeats.join(','));
            formData.append('query', `Confirm seats: ${selectedSeats.join(', ')}`);
            formData.append('sessionId', sessionId);

            // Add user info if logged in
            if (user?.email) {
                formData.append('userEmail', user.email);
            }

            const response = await fetch(`${API_URL}/chat/webhook`, {
                method: 'POST',
                body: formData
            });

            if (!response.ok) throw new Error('Failed to confirm seats');

            const data = await response.json();
            if (data.messages) {
                data.messages.forEach((msg, i) => {
                    setTimeout(() => {
                        addBotMessage(msg);
                    }, i * 1000);
                });
            }
        } catch (error) {
            console.error('Seat confirmation error:', error);
            addBotMessage("Failed to confirm seats. Please try again.");
        } finally {
            setLoading(false);
            setSelectedSeats([]);
            setSeatLayout([]);
        }
    };

    const handleAudioInput = async () => {
        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
            addBotMessage("Audio recording is not supported in your browser.");
            return;
        }

        if (isRecording) {
            if (recorderRef.current) {
                recorderRef.current.stopRecording(() => {
                    const blob = recorderRef.current.getBlob();
                    sendToBackend(blob, true);
                    setIsRecording(false);
                });
            }
            return;
        }

        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            recorderRef.current = new RecordRTC(stream, {
                type: 'audio',
                mimeType: 'audio/wav',
                numberOfAudioChannels: 1,
            });
            recorderRef.current.startRecording();
            setIsRecording(true);
            addUserMessage("🎤 Listening...");
        } catch (error) {
            console.error('Error accessing microphone:', error);
            addBotMessage("Please allow microphone access to use audio input.");
        }
    };

    const handleDateClick = () => {
        setShowDatePicker(true);
    };

    return (
        <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
            <div className="min-h-screen bg-gradient-to-br from-gray-900 via-cyan-900 to-gray-900">
                {/* NavBar with Auth - This is the only header */}
                <NavBar
                    onLoginSuccess={handleLoginSuccess}
                    onLogout={handleLogout}
                    user={user}
                />

                <div className="container mx-auto px-4 py-6 max-w-4xl">
                    <div className="bg-gradient-to-br from-gray-900/80 to-gray-800/90 backdrop-blur-xl rounded-2xl shadow-2xl border border-gray-700/50 p-6 h-[80vh] flex flex-col">
                        {/* Messages area with auto-scroll */}
                        <div className="flex-1 overflow-y-auto rounded-xl my-4 bg-gray-800/50 p-4">
                            <ChatMessages messages={messages} />
                            {loading && (
                                <div className="flex items-start gap-3 mb-4">
                                    <img
                                        src="https://api.dicebear.com/7.x/avataaars/svg?seed=MovieBot"
                                        alt="Bot"
                                        className="w-8 h-8 rounded-full"
                                    />
                                    <div className="message-content bg-gray-800 text-white p-3 rounded-lg">
                                        <strong className="text-cyan-300">Bot</strong>
                                        <TypingIndicator />
                                    </div>
                                </div>
                            )}
                            <div ref={messagesEndRef} />
                        </div>

                        {/* Modals */}
                        <DatePickerModal
                            visible={showDatePicker}
                            selectedDate={selectedDate}
                            onSelectDate={handleDateChange}
                            onClose={() => setShowDatePicker(false)}
                            onConfirm={handleDateConfirm}
                        />

                        <SeatPickerModal
                            visible={showSeatPicker}
                            seatLayout={seatLayout}
                            selectedSeats={selectedSeats}
                            parameters={{ maxSeats }}
                            onCancel={() => {
                                setShowSeatPicker(false);
                                setSelectedSeats([]);
                                addBotMessage("Seat selection cancelled.");
                            }}
                            onConfirm={handleSeatConfirm}
                            loading={loading}
                        />

                        {/* Input field */}
                        <div className="flex-shrink-0 bg-gray-800/50 backdrop-blur-sm rounded-xl p-4 border border-gray-600/30">
                            <ChatInput
                                input={input}
                                setInput={setInput}
                                onSend={handleSendMessage}
                                onMicClick={handleAudioInput}
                                onDateClick={handleDateClick}
                                isRecording={isRecording}
                                loading={loading}
                                inputRef={inputRef}
                            />
                        </div>
                    </div>
                </div>
            </div>
        </GoogleOAuthProvider>
    );
};

export default TicketBookingChatbot;