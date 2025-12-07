import React from "react";

const TypingIndicator = () => {
    return (
        <div className="flex gap-1">
            <span className="w-3 h-3 bg-cyan-400 rounded-full animate-pulse-dot-0"></span>
            <span className="w-3 h-3 bg-cyan-400 rounded-full animate-pulse-dot-1"></span>
            <span className="w-3 h-3 bg-cyan-400 rounded-full animate-pulse-dot-2"></span>
        </div>
    );
};

export default TypingIndicator;
