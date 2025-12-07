import React, { useState, useEffect } from "react";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faCheck } from "@fortawesome/free-solid-svg-icons";

const SeatPickerModal = ({
                             visible,
                             seatLayout = [],
                             selectedSeats: initialSelectedSeats = [],
                             parameters = { maxSeats: 1 },
                             onCancel,
                             onConfirm,
                             loading = false,
                         }) => {
    const [selectedSeats, setSelectedSeats] = useState(initialSelectedSeats);

    useEffect(() => {
        setSelectedSeats(initialSelectedSeats);
    }, [initialSelectedSeats]);

    const toggleSeatSelection = (seatId) => {
        if (selectedSeats.includes(seatId)) {
            setSelectedSeats(selectedSeats.filter((id) => id !== seatId));
        } else if (selectedSeats.length < parameters.maxSeats) {
            setSelectedSeats([...selectedSeats, seatId]);
        }
    };

    if (!visible) return null;

    return (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center z-50 p-4">
            <div className="bg-gray-900 p-6 rounded-2xl shadow-2xl w-full max-w-4xl max-h-[90vh] overflow-y-auto">
                <h3 className="text-2xl font-bold text-white text-center mb-6">
                    Select {parameters.maxSeats} Seat{parameters.maxSeats > 1 ? "s" : ""}
                    {selectedSeats.length > 0 && (
                        <span className="text-cyan-300 ml-2">
              ({selectedSeats.length}/{parameters.maxSeats} selected)
            </span>
                    )}
                </h3>

                {/* Screen */}
                <div className="mb-6 text-center">
                    <div className="bg-gradient-to-r from-cyan-500 to-blue-600 text-white py-3 mx-auto w-3/4 rounded-t-xl shadow-lg">
                        🎬 SCREEN 🎬
                    </div>
                </div>

                {/* Seats */}
                <div className="seat-layout overflow-auto max-h-[50vh] p-4 bg-gray-700/30 rounded-xl">
                    {seatLayout.map((row, rowIndex) => (
                        <div key={rowIndex} className="flex justify-center mb-3">
                            <div className="w-16 text-cyan-300 font-bold mr-3 flex items-center text-sm">
                                Row {String.fromCharCode(65 + rowIndex)}
                            </div>
                            <div className="flex gap-2 flex-wrap justify-center">
                                {row.map((seat) => {
                                    const isSelected = selectedSeats.includes(seat.id);
                                    return (
                                        <button
                                            key={seat.id}
                                            disabled={seat.booked || !seat.available || loading}
                                            onClick={() => toggleSeatSelection(seat.id)}
                                            className={`w-10 h-10 rounded-lg flex items-center justify-center text-xs font-bold transition-all duration-200 ${
                                                isSelected
                                                    ? "bg-gradient-to-r from-cyan-500 to-blue-600 text-white shadow-lg scale-110"
                                                    : seat.available
                                                        ? "bg-gray-600 hover:bg-gray-500 text-white hover:scale-105 hover:shadow-md cursor-pointer"
                                                        : "bg-red-500 text-white cursor-not-allowed opacity-70"
                                            }`}
                                            title={seat.booked ? "Booked" : seat.available ? "Available" : "Not available"}
                                        >
                                            {seat.number}
                                        </button>
                                    );
                                })}
                            </div>
                        </div>
                    ))}
                </div>

                {/* Footer */}
                <div className="flex justify-between items-center mt-8 flex-wrap gap-4">
                    <button
                        onClick={onCancel}
                        className="px-6 py-3 bg-gray-600 text-white rounded-xl hover:bg-gray-500 transition-all font-medium"
                    >
                        Cancel
                    </button>

                    <div className="flex items-center gap-4 flex-wrap">
                        {selectedSeats.length > 0 && (
                            <div className="text-cyan-300 font-medium bg-gray-700/50 px-4 py-2 rounded-lg">
                                Selected: <span className="text-white">{selectedSeats.join(", ")}</span>
                            </div>
                        )}

                        <button
                            onClick={() => onConfirm(selectedSeats)}
                            disabled={selectedSeats.length === 0 || loading}
                            className={`px-6 py-3 rounded-xl transition-all font-medium flex items-center gap-3 ${
                                selectedSeats.length > 0 && !loading
                                    ? "bg-gradient-to-r from-green-500 to-emerald-600 hover:shadow-lg text-white"
                                    : "bg-gray-500 text-gray-300 cursor-not-allowed"
                            }`}
                        >
                            {loading ? (
                                <>
                                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                                    Processing...
                                </>
                            ) : (
                                <>
                                    <FontAwesomeIcon icon={faCheck} />
                                    Confirm Seats
                                </>
                            )}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default SeatPickerModal;
