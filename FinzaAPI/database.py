# Caleb Mercier - 2025
# Completed with assistance from Chat GPT

from sqlalchemy import (
    Column, Integer, String, Text, Float, DateTime, ForeignKey, create_engine
)
from sqlalchemy.orm import declarative_base, sessionmaker, relationship
from sqlalchemy.sql import func

DATABASE_URL = "postgresql://caleb:2121@localhost:5432/finza_db"

engine = create_engine(DATABASE_URL)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


class User(Base):
    __tablename__ = "users"
    id = Column(Integer, primary_key=True, index=True)
    username = Column(String, unique=True, index=True)
    password_hash = Column(String)

    simulations = relationship("Simulation", back_populates="user")


class Simulation(Base):
    __tablename__ = "simulations"
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    input_json = Column(Text, nullable=False)
    result_json = Column(Text, nullable=False)
    success_probability = Column(Float, nullable=False)

    user = relationship("User", back_populates="simulations")
    recommendations = relationship("Recommendation", back_populates="simulation")


class Recommendation(Base):
    __tablename__ = "recommendations"
    id = Column(Integer, primary_key=True, index=True)
    simulation_id = Column(Integer, ForeignKey("simulations.id"), nullable=False)
    title = Column(String, nullable=False)
    description = Column(Text, nullable=False)
    impact_summary = Column(Text, nullable=False)

    simulation = relationship("Simulation", back_populates="recommendations")


# Create tables if they don't exist
Base.metadata.create_all(bind=engine)