import React, { useEffect, useRef } from "react";
import AnimatedMessage from "./AnimatedMessage.jsx";

const ChatMessages = ({ messages }) => {
    const messagesEndRef = useRef(null);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    };

    useEffect(() => {
        scrollToBottom();
    }, [messages]);

    return (
        <div className="flex-1 overflow-y-auto px-4 py-2">
            {messages.map((msg, idx) => (
                <div key={idx} className={`message flex items-start gap-3 mb-4 ${msg.sender === "You" ? "flex-row-reverse" : ""}`}>
                    <img
                        src={msg.avatar}
                        alt={msg.sender}
                        className={`w-8 h-8 rounded-full ${msg.sender === "You" ? "order-2" : ""}`}
                    />
                    <div className={`message-content ${msg.sender === "You" ? "bg-purple-700" : "bg-gray-800"} text-white p-3 rounded-lg max-w-[80%]`}>
                        <strong className={`block mb-1 ${msg.sender === "You" ? "text-purple-200" : "text-cyan-300"}`}>
                            {msg.sender}
                        </strong>
                        <AnimatedMessage text={msg.text} />
                    </div>
                </div>
            ))}
            {/* This invisible div will be used for auto-scrolling */}
            <div ref={messagesEndRef} />
        </div>
    );
};

export default ChatMessages;