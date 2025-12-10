# 🎬 Movie Ticket Booking Chatbot

[![React](https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)](https://reactjs.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![DialogFlow](https://img.shields.io/badge/DialogFlow-FF9800?style=for-the-badge&logo=dialogflow&logoColor=white)](https://cloud.google.com/dialogflow)
[![Vercel](https://img.shields.io/badge/Vercel-000000?style=for-the-badge&logo=vercel&logoColor=white)](https://vercel.com)
[![MongoDB](https://img.shields.io/badge/MongoDB-47A248?style=for-the-badge&logo=mongodb&logoColor=white)](https://www.mongodb.com/)

> An AI-powered conversational chatbot for booking movie tickets using natural voice/text interactions.

## 🎯 Overview

This project is a **full-stack web application** that allows users to book movie tickets through an intelligent chatbot interface. The system understands natural language queries, processes voice commands, and provides a seamless booking experience.

**🔗 Live Demo:** [https://movie-ticket-booking-chatbot.vercel.app](https://movie-ticket-booking-chatbot.vercel.app)

**⚡ Backend API:** [https://chatbot.development.catalystappsail.in](https://chatbot.development.catalystappsail.in)

## ✨ Features

### 🤖 AI & Conversational Features
- **Natural Language Processing** using Google DialogFlow
- **Voice-to-Text** conversion for hands-free interaction
- **Context-aware conversations** maintaining booking flow
- **Intent recognition** for movie queries, showtimes, and seat selection

### 🎟️ Booking & Management
- **Real-time seat selection** with visual seat map
- **Showtime browsing** by movie, location, and date
- **Multiple theater support** with availability checking
- **Booking history** and confirmation system

### 🔐 Security & Integration
- **Google OAuth 2.0** for secure authentication
- **Secure payment processing** via Cashfree gateway
- **Email notifications** for booking confirmations
- **CORS-enabled** for secure frontend-backend communication

## 🏗️ Architecture
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│ Vercel │────▶│ Zoho Catalyst │────▶│ MongoDB Atlas │
│ (Frontend) │ │ (Backend) │ │ (Database) │
└─────────────────┘ └─────────────────┘ └─────────────────┘
│ │ │
▼ ▼ ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│ React App │ │ Spring Boot │ │ Movie Data │
│ UI/UX │ │ REST API │ │ User Sessions │
└─────────────────┘ └─────────────────┘ └─────────────────┘
│
┌────────────────────┴────────────────────┐
▼ ▼ ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│ DialogFlow AI │ │ Google Speech │ │ Payment Gateway│
│ NLP Engine │ │ Voice-to-Text │ │ Cashfree │
└─────────────────┘ └─────────────────┘ └─────────────────┘

## 🛠️ Tech Stack

### Frontend
- **React 18** with functional components and hooks
- **Vite** for fast development and building
- **CSS3** with modern responsive design
- **Axios** for API communication
- **React Router** for navigation

### Backend
- **Java 17** with Spring Boot 3.x
- **Spring Web MVC** for REST API development
- **Spring Data MongoDB** for database operations
- **Spring Security** with OAuth 2.0
- **Google Cloud Java Client Libraries**

### AI & External Services
- **Google DialogFlow** for natural language processing
- **Google Cloud Speech-to-Text** for voice recognition
- **Google OAuth 2.0** for user authentication
- **TMDB API** for movie data and metadata
- **MovieGlu API** for showtimes and theater information
- **Cashfree Payment Gateway** for payment processing

### Database
- **MongoDB Atlas** (Cloud Database)
- **Document-based storage** for flexible data modeling
- **Collections**: Users, Bookings, Shows, Theaters

### Deployment
- **Frontend**: Vercel (Static hosting)
- **Backend**: Zoho Catalyst AppSail (PaaS)
- **Database**: MongoDB Atlas (Cloud)
- **CI/CD**: Manual deployment with environment-specific configurations

## 📁 Project Structure

movie-ticket-booking-chatbot/
│
├── frontend/ # React application (This repository)
│ ├── src/
│ │ ├── components/ # Reusable UI components
│ │ ├── pages/ # Page components
│ │ ├── services/ # API service calls
│ │ ├── utils/ # Utility functions
│ │ └── App.jsx # Main application component
│ ├── public/ # Static assets
│ ├── package.json # Dependencies
│ └── vite.config.js # Vite configuration
│
├── backend/ # Spring Boot application (Private repository)
│ ├── src/main/java/com/ticket/chatbot/
│ │ ├── controller/ # REST controllers
│ │ ├── model/ # Data models (Booking, User, Seat, etc.)
│ │ ├── repository/ # MongoDB repositories
│ │ ├── service/ # Business logic
│ │ ├── config/ # Configuration classes
│ │ └── ChatbotApplication.java
│ ├── pom.xml # Maven dependencies
│ └── application.properties
│
└── README.md # This file


## 🚀 Getting Started

### Prerequisites

- **Node.js** 16+ and **npm** for frontend
- **Java 17** and **Maven** for backend
- **MongoDB Atlas** account for database
- **Google Cloud** account for DialogFlow and Speech-to-Text
- **Zoho Catalyst** account for backend deployment
- **Vercel** account for frontend deployment

### Frontend Setup

```bash
# Clone the repository
git clone https://github.com/jehiel06/movie-ticket-booking-chatbot.git
cd movie-ticket-booking-chatbot/frontend

# Install dependencies
npm install

# Set environment variables
cp .env.example .env.local
# Edit .env.local with your API endpoints

# Start development server
npm run dev

# Build for production
npm run build
