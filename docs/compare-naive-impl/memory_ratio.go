package main

import (
	"fmt"
	"math"
)

func main() {
	ps := []float64{
		0.01,
		0.001,
		0.0001,
		0.00001,
		0.000001,
	}
	ns := []float64{
		0,
		32,
		128,
		1024,
	}

	fmt.Print("誤り率\\要素長")
	for _, n := range ns {
		fmt.Printf(" | %.0f バイト", n)
	}
	fmt.Println()

	fmt.Print("---:")
	for range ns {
		fmt.Print("|---:")
	}
	fmt.Println()

	for _, p := range ps {
		fmt.Printf("%g", p)
		for _, n := range ns {
			r := (54 + n) / (4.8 * -(math.Log10(p)))
			fmt.Printf(" | %.2f", r)
		}
		fmt.Println()
	}
}
