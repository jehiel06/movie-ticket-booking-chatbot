import React from "react";

const DatePickerModal = ({ visible, selectedDate, onClose, onSelectDate, onConfirm }) => {
    if (!visible) return null;

    // Helper to format date as YYYY-MM-DD
    const formatDate = (date) => {
        if (!date) return "";
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    };

    return (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
            <div className="bg-gray-800 p-6 rounded-2xl w-full max-w-md">
                <h3 className="text-xl text-white mb-4 text-center">Select Travel Date</h3>

                {/* Show what will be sent to backend */}
                {selectedDate && (
                    <div className="mb-4 p-3 bg-gray-700 rounded-lg">
                        <p className="text-white text-center text-sm">
                            Will send to backend:
                        </p>
                        <p className="text-cyan-300 text-center font-mono text-lg">
                            {formatDate(selectedDate)}
                        </p>
                    </div>
                )}

                <input
                    type="date"
                    min={new Date().toISOString().split("T")[0]}
                    className="w-full p-3 bg-gray-700 rounded-lg text-white mb-4"
                    onChange={(e) => onSelectDate(new Date(e.target.value))}
                />

                <div className="flex justify-between mt-4">
                    <button
                        onClick={onClose}
                        className="bg-gray-600 px-4 py-2 rounded-lg text-white hover:bg-gray-500 transition-colors"
                    >
                        Cancel
                    </button>

                    {selectedDate && (
                        <button
                            onClick={onConfirm}
                            className="bg-green-600 px-4 py-2 rounded-lg text-white hover:bg-green-700 transition-colors"
                        >
                            Confirm Date
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
};

export default DatePickerModal;