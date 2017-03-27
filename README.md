# StackMatGen3TimerAPI
StackMat Generation 3 Timer API

## YuXin Timer

The software works with the "YuXin Cube Timer V1", too. This piece of hardware can be connected to your computer's microphone input via a simple cable: On the timer it's a 2.5mm TS (mono) audio jack, for the PC side you need a 3.5mm audio jack (mono / TS or stereo / TRS). When you use a stereo jack, make sure to connect the **center** (Tip)! Initially, I chose the Sleeve which is not picked up by the software.

The current time display is emitted at regular intervals, in a 10-byte ASCII telegram that totals about 80ms, in what looks like standard serial 12008N1 communication, least significant bit first, low voltage "1", high voltage "0":

- One *instruction* byte
  * For some reason **my** Yuxin timer only ever sends the "S" instruction (0x53).
  * I do _not_ receive A, L, R, C, or Space instructions.
  * As a consequence, [PrismaPuzzleTimer](https://github.com/phillip-hayes/PrismaPuzzleTimer) does show the elapsed time, but is unable to detect the start and end of a solve (!!)
  * Since I only have a single unit that I have already tinkered with I cannot be sure if that's not a home-made malfunction, so __maybe__ Yuxin are not to blame, might well be my own fault.
- six digits, representing the time ("0" = 0x30 ... "9" = 0x39)
- one check digit
- one LF (0x0A)
- one CR (0x0D)

For illustration, here is a plot of how "6.189" gets communicated as "S-006189-X-LF-CR". See the Yuxin folder for more images. This was taken off a unit with PCB date "2016-12-14" using [Soundcard Scope](https://www.zeitnitz.eu/scms/scope_en). To safeguard my PC against potentially harmful input signals, I whipped up a protection circuit with two antiparallel diodes and two resistors as a voltage divider.

![YuXin Cube Timer V1 S-006189-X-LF-CR](yuxin/scope-006189_bw.jpg "YuXin Cube Timer V1 S-006189-X-LF-CR")

Happy cubing!
 
