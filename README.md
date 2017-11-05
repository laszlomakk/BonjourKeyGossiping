# BonjourKeyGossiping
Android prototype for Local Key Gossiping

## Intro

This project aims to explore and prototype gossiping public keys on a local wifi network.
The Android (and later probably also iOS) application proposed by the project
would aim to share public keys between users when their devices are connected to the same wifi network
(just by running in the background), and detect key changes.
It would do this while protecting the users’ privacy to the fullest extent possible,
and also in a highly automated way, possibly without any user interaction.
The intention of the overall project is to create a key gossiping library
that can audit the honesty of service providers (key servers),
and can be easily integrated into existing secure messaging applications such as Signal or WhatsApp.

For a detailed description of the project and this prototype in particular, see the `documentation` folder.

## Note

The work here was done June-Sep 2016 as part of my internship at the Computer Laboratory, University of Cambridge.
Many of the ideas and overall functionality was devised together with my supervisors, Dr Alastair Beresford and Diana Vasile.
