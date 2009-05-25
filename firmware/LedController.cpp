#include "LedController.h"
#include "Characters.h"
#include <stdlib.h>
#include <avr/io.h>
#include <avr/interrupt.h>
#include <wiring.h>
#include "device.h"

#if defined BLIPBOX_P2

uint8_t rowOffsets[] = {
  2, 1, 0, 3, 4, 
  3, 1, 0, 2, 4 
};

uint8_t colOffsets[] = {
  6, 4, 7, 5, 1, 3, 0, 2, 
  10, 8, 11, 9, 13, 15, 12, 14
};

#elif defined BLIPBOX_P4

uint8_t rowOffsets[] = {
  4, 0, 1, 2, 3,
  4, 0, 1, 2, 3
};

uint8_t colOffsets[] = {
  8, 9, 10, 11, 12, 13, 14, 15,
  0, 1,  2,  3,  4,  5,  6,  7
};

#elif defined BLIPBOX_V6

uint8_t rowOffsets[] = {
  4, 0, 1, 2, 3,
  4, 0, 1, 2, 3
};

uint8_t colOffsets[] = {
  8, 9, 10, 11, 12, 13, 14, 15,
  0, 1, 2, 3, 4, 5, 6, 7
};

#endif /* BLIPBOX_xx */

#ifdef TLC_VPRG_PIN
/* send 6 bits from an 8 bit value over the TLC5940 data line */
static void shift6bits(uint8_t value) {
  for (uint8_t i=0x20;i;i>>=1){
    if(value & i)
      TLC_SIN_PORT |= _BV(TLC_SIN_PIN);
    else
      TLC_SIN_PORT &= ~_BV(TLC_SIN_PIN);
    TLC_SCLK_PORT |= _BV(TLC_SCLK_PIN);
    TLC_SCLK_PORT &= ~_BV(TLC_SCLK_PIN);
  }
}
#endif

/* send 10 bits over the TLC5940 data line using an 8 bit value 
   and 4 (least significant) bits padding */
static void shift12bits(uint8_t value) {
  for(uint8_t i=0x80; i; i>>=1){
    if(value & i)
      TLC_SIN_PORT |= _BV(TLC_SIN_PIN);
    else
      TLC_SIN_PORT &= ~_BV(TLC_SIN_PIN);
    // clock
    TLC_SCLK_PORT |= _BV(TLC_SCLK_PIN);
    TLC_SCLK_PORT &= ~_BV(TLC_SCLK_PIN);
  }
  // shift out four more zeros or ones for the 4 least significant bits
  if(value)
    TLC_SIN_PORT |= _BV(TLC_SIN_PIN);
  else
    TLC_SIN_PORT &= ~_BV(TLC_SIN_PIN);
  for(uint8_t i=0; i<4; ++i){
    TLC_SCLK_PORT |= _BV(TLC_SCLK_PIN);
    TLC_SCLK_PORT &= ~_BV(TLC_SCLK_PIN);
  }
}

void LedController::setDiagonalCross(uint8_t row, uint8_t col, uint8_t value){
  //   row goes to 10, ie row is x coordinate on screen
//   uint8_t d1 = row - col;
//   uint8_t d2 = 7 - row - col;
//   for(uint8_t i=0; i<8; ++i){
//     if(i+d1 >= 0 && i+d1 < 10)
// //       setLed(i+d1, i, value);
//       setLed(i+d1, i, value * abs(i-col) / 7);
//     if(i-d2 >= 0 && i-d2 < 10)
// //       setLed(i-d2, 7-i, value);
//       setLed(i-d2, 7-i, value * abs(7-i-col) / 7);
//   }
  for(int8_t i=-8; i<8; ++i){
    setLed(row+i, col+i, value / (2*abs(i) + 1));
    setLed(row-i, col+i, value / (2*abs(i) + 1));
  }
}

// todo: set blob with 10 or 8 bit precision location
void LedController::setBlob(uint8_t row, uint8_t col, uint8_t value){
  for(int8_t i=-3; i<4; i+=2){
    setLed(row+i, col+i, value / (2*abs(i) + 1));
    setLed(row+i, col-i, value / (2*abs(i) + 1));
    setLed(row-i, col+i, value / (2*abs(i) + 1));
    setLed(row-i, col-i, value / (2*abs(i) + 1));
  }
}

void LedController::setCross(uint8_t row, uint8_t col, uint8_t value){
  for(uint8_t i=0; i<10; ++i)
    setLed(i, col, value / (4*abs(row-i) + 1));
  for(uint8_t i=0; i<8; ++i)
    setLed(row, i, value / (4*abs(col-i) + 1));
}

void LedController::setStar(uint8_t row, uint8_t col, uint8_t value){
  setLed(row+1, col, value);
  setLed(row, col+1, value);
  setLed(row-1, col, value);
  setLed(row, col-1, value);
}

void LedController::setSquare(uint8_t row, uint8_t col, uint8_t value){
  setStar(row, col, value);
  value >>= 1;
  setLed(row+1, col+1, value);
  setLed(row-1, col+1, value);
  setLed(row+1, col-1, value);
  setLed(row-1, col-1, value);
}

void LedController::setLed(uint8_t row, uint8_t col, uint8_t value){
  if(col < 8){
    if(row < 5)
      leds[rowOffsets[row]].setLed(colOffsets[col], value);
    else if(row < 10)
      leds[rowOffsets[row]].setLed(colOffsets[col+8], value);
  }
}

uint8_t LedController::getLed(uint8_t row, uint8_t col){
  if(col < 8){
    if(row < 5)
      return leds[rowOffsets[row]].values[colOffsets[col]];
    else if(row < 10)
      return leds[rowOffsets[row]].values[colOffsets[col+8]];
  }
  return 0;
}

/* set the dot correction value for all pins to a value between 0 and 63 */
#ifdef TLC_VPRG_PIN
void LedController::setGlobalDotCorrection(uint8_t value) {
  TLC_VPRG_PORT |= _BV(TLC_VPRG_PIN);
  for(uint8_t i=0;i<16;i++)
    shift6bits(value);
  TLC_XLAT_PORT |= _BV(TLC_XLAT_PIN);
  TLC_XLAT_PORT &= ~_BV(TLC_XLAT_PIN);
  TLC_VPRG_PORT &= ~_BV(TLC_VPRG_PIN);
  doExtraPulse = true;
  counter.reset();
}
#endif

void LedController::init() {

  counter.init();
  counter.reset();

  cli();
  // set pins to output
  TLC_XLAT_DDR |= _BV(TLC_XLAT_PIN);
  TLC_BLANK_DDR |= _BV(TLC_BLANK_PIN);
  TLC_GSCLK_DDR |= _BV(TLC_GSCLK_PIN);

  TLC_SIN_DDR |= _BV(TLC_SIN_PIN);
  TLC_SCLK_DDR |= _BV(TLC_SCLK_PIN);

 // set blank high
  TLC_BLANK_PORT |= _BV(TLC_BLANK_PIN);
  // put everything else low until ready
  TLC_XLAT_PORT &= ~_BV(TLC_XLAT_PIN);
  TLC_SCLK_PORT &= ~_BV(TLC_SCLK_PIN);
  TLC_GSCLK_PORT &= ~_BV(TLC_GSCLK_PIN);

#ifdef TLC_VPRG_PIN
  TLC_VPRG_DDR  |= _BV(TLC_VPRG_PIN);
  setGlobalDotCorrection(63); // Max intensity.
#endif

  // PWM timer
  TCCR2A = (_BV(WGM21) |   // CTC 
            _BV(COM2A0));  // toggle OC2A on match -> GSCLK
  TCCR2B = _BV(CS20);      // No prescaler
  OCR2A = 1;               // toggle every timer clock cycle -> 4 MHz
  TCNT2 = 0;

  // Latch timer
  TCCR1A = (_BV(WGM10));   // Fast PWM 8-bit
  
  // div 64 prescaler
  // Fast PWM 8-bit  : 1/4096th of OC2A
  TCCR1B = (_BV(CS11) | _BV(CS10) | _BV(WGM12));

  // Enable overflow interrupt
  TIMSK1 = _BV(TOIE1);     
  TCNT1 = 0;

//   // Timer 1 - BLANK / XLAT
// #define TLC_PWM_PERIOD    8192
//     TCCR1A = _BV(COM1B1);  // non inverting, output on OC1B, BLANK
//     TCCR1B = _BV(WGM13);   // Phase/freq correct PWM, ICR1 top
//     OCR1A = 1;             // duty factor on OC1A, XLAT is inside BLANK
//     OCR1B = 2;             // duty factor on BLANK (larger than OCR1A (XLAT))
//     ICR1 = TLC_PWM_PERIOD; // see tlc_config.h

//     // Timer 2 - GSCLK
// #define TLC_GSCLK_PERIOD    3
//     TCCR2A = _BV(COM2B1)      // set on BOTTOM, clear on OCR2A (non-inverting),
//                               // output on OC2B
//            | _BV(WGM21)       // Fast pwm with OCR2A top
//            | _BV(WGM20);      // Fast pwm with OCR2A top
//     TCCR2B = _BV(WGM22);      // Fast pwm with OCR2A top
//     OCR2B = 0;                // duty factor (as short a pulse as possible)
//     OCR2A = TLC_GSCLK_PERIOD; // see tlc_config.h
//     TCCR2B |= _BV(CS20);      // no prescale, (start pwm output)
//     TCCR1B |= _BV(CS10);      // no prescale, (start pwm output)

  sei();
}

void LedController::brighten(uint8_t factor){
  for(uint8_t row=0; row<ROWS; ++row)
    for(uint8_t i=0;i<FRAME_LENGTH;i++)
      leds[row].values[i] <<= factor;
}

void LedController::fade(uint8_t factor){
  for(uint8_t row=0; row<ROWS; ++row)
    for(uint8_t i=0;i<FRAME_LENGTH;i++)
      leds[row].values[i] >>= factor;
//       leds[row].values[i] = leds[row].values[i] > factor ? leds[row].values[i] - factor : 0;
}

void LedController::clear(){
  for(uint8_t row=0; row<ROWS; ++row)
    leds[row].clear();
}

void LedRow::clear(){
  for(uint8_t i=0;i<FRAME_LENGTH;i++)
    values[i] = 0;
}

  // shifts the led data in the given direction
void LedController::shift(uint8_t direction){
  // the leftmost 2 bits determine the direction: 0: left, 1: right, 2: up, 3: down
  // the rightmost 2 bits determines the number of steps: 1-4
  switch(direction & 0xc){
  case 0x0: // shift left
    for(uint8_t col=0; col<7; ++col)
      for(uint8_t row=0; row<10; ++row)
        setLed(row, col, getLed(row, col+1));
    for(uint8_t row=0; row<10; ++row)
      setLed(row, 7, 0);
    break;
  case 0x4: // shift right
    for(uint8_t col=7; col>0; --col)
      for(uint8_t row=0; row<10; ++row)
        setLed(row, col, getLed(row, col-1));
    for(uint8_t row=0; row<10; ++row)
      setLed(row, 0, 0);
    break;
  case 0x8:
    for(uint8_t col=0; col<8; ++col)
      for(uint8_t row=0; row<9; ++row)
        setLed(row, col, getLed(row+1, col));
    for(uint8_t col=0; col<8; ++col)
      setLed(9, col, 0);
    break;
  case 0xc:
    for(uint8_t col=0; col<8; ++col)
      for(uint8_t row=9; row>0; --row)
        setLed(row, col, getLed(row-1, col));
    for(uint8_t col=0; col<8; ++col)
      setLed(0, col, 0);
    break;
  }
}

void LedController::printCharacter(uint8_t* character, uint8_t row, uint8_t col, uint8_t brightness){
  // row goes from 0-9, col from 0-7
  for(int i=0; i<getCharacterHeight(); ++i){
    for(int j=8-getCharacterWidth(); j<8; ++j){
      // only shift out the relevant bits
      if(character[i] & _BV(j))
        setLed(7-j+row, col+i, brightness);
      else
        setLed(7-j+row, col+i, 0x00);
    }
  }
}

void LedController::displayCurrentRow() {
  const LedRow& row = leds[counter.getCurrentRow()];
  // shift data out

//     if(firstGSInput) {
//         // adds an extra SCLK pulse unless we've just set dot-correction data
//         firstGSInput = 0;
//     } else {
//       TLC_SCLK_PORT |= _BV(TLC_SCLK_PIN);
//       TLC_SCLK_PORT &= ~_BV(TLC_SCLK_PIN);
//     }

  TLC_SCLK_PORT |= _BV(TLC_SCLK_PIN);
  TLC_SCLK_PORT &= ~_BV(TLC_SCLK_PIN);

  for(uint8_t i=0;i<FRAME_LENGTH;i++)
    shift12bits(row.values[i]);

  // blank
  TLC_BLANK_PORT |= _BV(TLC_BLANK_PIN);

  counter.increment();

  // latch
  TLC_XLAT_PORT |= _BV(TLC_XLAT_PIN);
  TLC_XLAT_PORT &= ~_BV(TLC_XLAT_PIN);
    
  // Extra SCLK pulse according to Datasheet p.15:
// The first GS data input cycle after dot correction requires an additional SCLK 
// pulse after the XLAT signal to complete the grayscale update cycle.
  if(doExtraPulse){
    TLC_SCLK_PORT |= _BV(TLC_SCLK_PIN);
    TLC_SCLK_PORT &= ~_BV(TLC_SCLK_PIN);
    doExtraPulse = false;
  }

  // unblank
  TLC_BLANK_PORT &= ~_BV(TLC_BLANK_PIN);
}
