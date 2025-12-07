import React, { useState, useEffect } from "react";

const LoaderTypingDots = () => {
    const [dots, setDots] = useState(".");

    useEffect(() => {
        const interval = setInterval(() => {
            setDots((prev) => (prev === "..." ? "." : prev + "."));
        }, 500);

        return () => clearInterval(interval);
    }, []);

    return <div className="text-gray-400 px-4 py-2">{dots}</div>;
};

export default LoaderTypingDots;
