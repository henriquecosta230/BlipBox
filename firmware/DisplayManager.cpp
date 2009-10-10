#include "DisplayManager.h"

#include <stdlib.h>

#include "globals.h"
#include "Characters.h"

  // shifts the led data in the given direction
void DisplayManager::shift(uint8_t direction){
  // the leftmost 2 bits determine the direction: 0: left, 1: right, 2: up, 3: down
  // the rightmost 2 bits determines the number of steps: 1-4
  switch(direction & 0xc){
  case 0x0: // shift left
    for(uint8_t col=7; col>0; --col)
      for(uint8_t row=0; row<10; ++row)
        leds.setLed(row, col, leds.getLed(row, col-1));
    for(uint8_t row=0; row<10; ++row)
      leds.setLed(row, 0, 0);
    break;
  case 0x4: // shift right
    for(uint8_t col=0; col<7; ++col)
      for(uint8_t row=0; row<10; ++row)
        leds.setLed(row, col, leds.getLed(row, col+1));
    for(uint8_t row=0; row<10; ++row)
      leds.setLed(row, 7, 0);
    break;
  case 0x8:
    for(uint8_t col=0; col<8; ++col)
      for(uint8_t row=0; row<9; ++row)
        leds.setLed(row, col, leds.getLed(row+1, col));
    for(uint8_t col=0; col<8; ++col)
      leds.setLed(9, col, 0);
    break;
  case 0xc:
    for(uint8_t col=0; col<8; ++col)
      for(uint8_t row=9; row>0; --row)
        leds.setLed(row, col, leds.getLed(row-1, col));
    for(uint8_t col=0; col<8; ++col)
      leds.setLed(0, col, 0);
    break;
  }
}

void DisplayManager::printCharacter(uint8_t* character, uint8_t row, uint8_t col, uint8_t brightness){
  // writing to row (horizontal offset) 0 puts character furthest right
  // row 5 is furthest left (while fitting 5 pixels)

  // row goes from 0-9, col from 0-7
  // font height/width = 8/5 for 6x9 font
  for(int i=0; i<getCharacterHeight(); ++i){
    // font data is 8 bits left adjusted
    uint8_t offset = 8 - getCharacterWidth();
    for(int j=0; j<getCharacterWidth(); ++j){
      // only shift out the relevant bits
      if(character[i] & _BV(j+offset))
        leds.setLed(j+row, i+col, brightness);
      else
        leds.setLed(j+row, i+col, 0x00);
    }
  }
}

void DisplayManager::setDiagonalCross(uint8_t row, uint8_t col, uint8_t value){
  //   row goes to 10, ie row is x coordinate on screen
//   uint8_t d1 = row - col;
//   uint8_t d2 = 7 - row - col;
//   for(uint8_t i=0; i<8; ++i){
//     if(i+d1 >= 0 && i+d1 < 10)
// //       leds.setLed(i+d1, i, value);
//       leds.setLed(i+d1, i, value * abs(i-col) / 7);
//     if(i-d2 >= 0 && i-d2 < 10)
// //       leds.setLed(i-d2, 7-i, value);
//       leds.setLed(i-d2, 7-i, value * abs(7-i-col) / 7);
//   }
  for(int8_t i=-8; i<8; ++i){
    leds.setLed(row+i, col+i, value / (2*abs(i) + 1));
    leds.setLed(row-i, col+i, value / (2*abs(i) + 1));
  }
}

// todo: set blob with 10 or 8 bit precision location
void DisplayManager::setBlob(uint8_t row, uint8_t col, uint8_t value){
  for(int8_t i=-3; i<4; i+=2){
    leds.setLed(row+i, col+i, value / (2*abs(i) + 1));
    leds.setLed(row+i, col-i, value / (2*abs(i) + 1));
    leds.setLed(row-i, col+i, value / (2*abs(i) + 1));
    leds.setLed(row-i, col-i, value / (2*abs(i) + 1));
  }
}

void DisplayManager::setCross(uint8_t row, uint8_t col, uint8_t value){
  for(uint8_t i=0; i<10; ++i)
    leds.setLed(i, col, value / (4*abs(row-i) + 1));
  for(uint8_t i=0; i<8; ++i)
    leds.setLed(row, i, value / (4*abs(col-i) + 1));
}

void DisplayManager::setStar(uint8_t row, uint8_t col, uint8_t value){
  leds.setLed(row+1, col, value);
  leds.setLed(row, col+1, value);
  leds.setLed(row-1, col, value);
  leds.setLed(row, col-1, value);
}

void DisplayManager::setSquare(uint8_t row, uint8_t col, uint8_t value){
  setStar(row, col, value);
  value >>= 1;
  leds.setLed(row+1, col+1, value);
  leds.setLed(row-1, col+1, value);
  leds.setLed(row+1, col-1, value);
  leds.setLed(row-1, col-1, value);
}