import React, { useState, useEffect, useRef } from "react";

const AnimatedMessage = ({ text }) => {
    const [displayedText, setDisplayedText] = useState("");
    const indexRef = useRef(0);
    const textRef = useRef(text);

    useEffect(() => {
        setDisplayedText("");
        indexRef.current = 0;
        textRef.current = text;

        if (!text) return;

        const interval = setInterval(() => {
            setDisplayedText((prev) => {
                if (indexRef.current >= textRef.current.length) {
                    clearInterval(interval);
                    return prev;
                }
                const nextChar = textRef.current.charAt(indexRef.current);
                indexRef.current += 1;
                return prev + nextChar;
            });
        }, 15);

        return () => clearInterval(interval);
    }, [text]);

    return (
        <div className="whitespace-pre-wrap break-words">
            {displayedText || text}
        </div>
    );
};

export default AnimatedMessage;