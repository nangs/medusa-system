% Filtr Butterwortha
%   >> Hf = butterworth(H, F, Fo, n)
% Oznaczenia:
%   H   - transformata Fouriera
%   F   - widmo częstotliwości
%   Fo  - częstotliwość odcięcia
%   n   - stopień filtra
function [Hf, Fb] = butterworth(Hp, F, Fo, n)
    Fb = 1 ./ (1 + ((F/Fo).^(2*n)));
    Hno = fftshift(Hp);
    Hf = Hno .* Fb;


