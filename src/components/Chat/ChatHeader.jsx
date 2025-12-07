import React from "react";

const ChatHeader = ({ botAvatar }) => (
    <header className="flex items-center gap-3 bg-gradient-to-r from-cyan-500 to-blue-600 rounded-xl p-4 shadow-lg mb-4">
        <img src={botAvatar} alt="Movie Ticket Bot" className="w-12 h-12 rounded-full border-2 border-white" />
        <div>
            <h1 className="text-white text-lg font-bold">Movie Ticket Booking Bot</h1>
            <p className="text-cyan-200 text-sm">Your Personal Movie Assistant</p>
        </div>
    </header>
);

export default ChatHeader;