import React from "react";

const ChatInput = ({
                       input,
                       setInput,
                       onSend,
                       onMicClick,
                       onDateClick,
                       isRecording,
                       loading,
                       inputRef,
                   }) => {
    const handleInputChange = (e) => {
        setInput(e.target.value);
    };

    return (
        <form
            onSubmit={onSend}
            className="mt-4 flex items-center gap-2 px-2 bg-gray-800 p-2 rounded-b-xl"
        >
            <button
                type="button"
                onClick={onMicClick}
                className={`flex-shrink-0 p-3 rounded-full transition-all duration-300 ${
                    isRecording
                        ? "bg-red-500 animate-pulse shadow-lg ring-2 ring-red-400"
                        : "bg-cyan-600 hover:bg-cyan-700 hover:shadow-md"
                }`}
                title={isRecording ? "Stop recording" : "Start voice input"}
                disabled={loading}
            >
                🎤
            </button>

            <input
                ref={inputRef}
                type="text"
                value={input}
                onChange={handleInputChange}
                placeholder="Ask about movies, showtimes, or book tickets..."
                className="flex-1 min-w-0 rounded-full bg-gray-700 text-white px-4 py-3 placeholder-gray-400 focus:ring-2 focus:ring-cyan-500 focus:outline-none transition-all border border-gray-600"
                disabled={loading}
                onKeyDown={(e) => {
                    if (e.key === 'Enter' && !e.shiftKey) {
                        e.preventDefault();
                        if (input.trim() && !loading) {
                            onSend(e);
                        }
                    }
                }}
                autoComplete="off"
                spellCheck="true"
                autoCorrect="on"
            />

            <button
                type="button"
                onClick={onDateClick}
                className="flex-shrink-0 p-3 rounded-full bg-green-600 hover:bg-green-700 hover:shadow-md transition-all duration-300"
                title="Select date"
                disabled={loading}
            >
                📅
            </button>

            <button
                type="submit"
                disabled={loading || !input.trim()}
                className="flex-shrink-0 p-3 rounded-full bg-purple-600 hover:bg-purple-700 disabled:opacity-50 disabled:cursor-not-allowed hover:shadow-md transition-all duration-300"
                title="Send message"
            >
                {loading ? "⏳" : "➤"}
            </button>
        </form>
    );
};

export default ChatInput;