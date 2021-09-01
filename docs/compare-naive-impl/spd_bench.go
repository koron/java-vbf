package main

// 2021-09-01:
// 非パイプライン下でSETEXにかかる時間: 約600μ秒/件

import (
	"bufio"
	"context"
	"crypto/rand"
	"errors"
	"fmt"
	"io"
	"log"
	"strconv"
	"strings"
	"time"

	"github.com/go-redis/redis/v8"
)

func main() {
	err := run()
	if err != nil {
		log.Fatal(err)
	}
}

type Info map[string]string

func (m Info) getInt64(name string) (int64, error) {
	v, ok := m[name]
	if !ok {
		return 0, fmt.Errorf("no values for key %q", name)
	}
	n, err := strconv.ParseInt(v, 10, 64)
	if err != nil {
		return 0, err
	}
	return n, nil
}

func parseInfo(s string) (Info, error) {
	r := bufio.NewReader(strings.NewReader(s))
	info := Info{}
	for {
		l, err := r.ReadString('\n')
		if err != nil {
			if errors.Is(err, io.EOF) {
				return info, nil
			}
			return nil, err
		}
		l = strings.TrimRight(l, "\r\n")
		if l == "" || strings.HasPrefix(l, "#") {
			continue
		}
		x := strings.IndexRune(l, ':')
		if x < 0 {
			return nil, errors.New("no semi-colon in info")
		}
		k, v := l[:x], l[x+1:]
		info[k] = v
	}
}

func getMem(ctx context.Context, uc redis.UniversalClient) (int64, error) {
	r, err := uc.Info(ctx, "memory").Result()
	if err != nil {
		return 0, err
	}
	v, err := parseInfo(r)
	if err != nil {
		return 0, err
	}
	return v.getInt64("used_memory")
}

func delKeys(ctx context.Context, uc redis.UniversalClient, pattern string) error {
	keys, err := uc.Keys(ctx, pattern).Result()
	if err != nil {
		return err
	}
	if len(keys) == 0 {
		return nil
	}
	p := uc.Pipeline()
	for len(keys) > 0 {
		n := 100
		if len(keys) < n {
			n = len(keys)
		}
		p.Del(ctx, keys[:n]...)
		keys = keys[n:]
	}
	_, err = p.Exec(ctx)
	return err
}

func genKey(prefix string, n int) string {
	b := make([]byte, n/2)
	rand.Read(b)
	return fmt.Sprintf("%s%x", prefix, b)
}

func run() error {
	opts, err := redis.ParseURL("redis://127.0.0.1:6379/0")
	if err != nil {
		return err
	}
	c := redis.NewClient(opts)
	defer c.Close()
	ctx := context.Background()

	measureSpeed(ctx, c, 1000, 32)

	measureSpeed(ctx, c, 100, 32)
	measureSpeed(ctx, c, 200, 32)
	measureSpeed(ctx, c, 400, 32)
	measureSpeed(ctx, c, 800, 32)
	measureSpeed(ctx, c, 1600, 32)
	measureSpeed(ctx, c, 3200, 32)
	measureSpeed(ctx, c, 6400, 32)
	measureSpeed(ctx, c, 12800, 32)
	//measureSpeed(ctx, c, 1000, 32)
	//measureSpeed(ctx, c, 10000, 32)

	return nil
}

func measureSpeed(ctx context.Context, uc redis.UniversalClient, n int, keyLen int) (int64, error) {
	log.Printf("start n=%d keyLen=%d", n, keyLen)

	err := delKeys(ctx, uc, "f_*")
	if err != nil {
		return 0, err
	}

	st := time.Now()
	for i := 0; i < n; i++ {
		k := genKey("f_", keyLen)
		_, err := uc.SetEX(ctx, k, 0, 24*time.Hour).Result()
		if err != nil {
			return 0, err
		}
	}
	d := time.Since(st)
	log.Printf("  duration %s (%s/set)", d, d/time.Duration(n))
	if err != nil {
		return 0, err
	}

	err = delKeys(ctx, uc, "f_*")
	if err != nil {
		return 0, err
	}

	return 0, nil
}
