import React from "react";
import { GoogleLogin } from "@react-oauth/google";
import { jwtDecode } from "jwt-decode";

const NavBar = ({ onLoginSuccess, onLogout, user }) => {
    const handleGoogleSuccess = (credentialResponse) => {
        const decoded = jwtDecode(credentialResponse.credential);
        onLoginSuccess(credentialResponse.credential, decoded);
    };

    const handleGoogleError = () => {
        console.log("Google Login Failed");
    };

    return (
        <nav className="sticky top-0 z-50 bg-gradient-to-r from-cyan-700 to-blue-800 backdrop-blur-md shadow-lg rounded-b-2xl">
            <div className="max-w-7xl mx-auto px-4 py-3 flex items-center justify-between">
                {/* Logo/Brand with Bot Info */}
                <div className="flex items-center gap-4">
                    <div className="flex items-center gap-3">
                        <img
                            src="https://api.dicebear.com/7.x/avataaars/svg?seed=MovieBot"
                            alt="Movie Ticket Bot"
                            className="w-10 h-10 rounded-full border-2 border-white"
                        />
                        <div>
                            <h1 className="text-white text-xl font-bold">Movie Ticket Bot</h1>
                            <p className="text-cyan-200 text-sm">Your Personal Movie Assistant</p>
                        </div>
                    </div>
                </div>

                {/* User/Auth Section */}
                <div className="flex items-center gap-4">
                    {user ? (
                        <div className="flex items-center gap-3">
                            {/* User Info */}
                            <div className="flex items-center gap-2 bg-white/10 px-3 py-2 rounded-full backdrop-blur-sm border border-white/20">
                                {user.picture ? (
                                    <img
                                        src={user.picture}
                                        alt={user.name}
                                        className="w-8 h-8 rounded-full border-2 border-white"
                                    />
                                ) : (
                                    <div className="w-8 h-8 rounded-full bg-gradient-to-r from-cyan-400 to-blue-600 flex items-center justify-center text-white font-semibold">
                                        {user.name?.charAt(0)?.toUpperCase()}
                                    </div>
                                )}
                                <span className="text-white font-medium text-sm">{user.name}</span>
                            </div>

                            {/* Logout Button */}
                            <button
                                onClick={onLogout}
                                className="px-4 py-2 bg-gradient-to-r from-red-500 to-pink-600 rounded-full text-white font-medium text-sm hover:shadow-lg hover:scale-105 transition-all"
                            >
                                Logout
                            </button>
                        </div>
                    ) : (
                        <GoogleLogin
                            onSuccess={handleGoogleSuccess}
                            onError={handleGoogleError}
                            theme="filled_blue"
                            size="medium"
                            shape="pill"
                            text="signin_with"
                        />
                    )}
                </div>
            </div>
        </nav>
    );
};

export default NavBar;