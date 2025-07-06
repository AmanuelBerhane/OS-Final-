#include <stdio.h>
#include <pthread.h>
#include <unistd.h>
#include <stdlib.h>
#include <time.h>

#define NUM_PHILOSOPHERS 5
#define MAX_CYCLES 10  // For demonstration purposes

// Philosopher states
typedef enum { THINKING, HUNGRY, EATING } State;

// Shared data structure
struct {
    State state[NUM_PHILOSOPHERS];
    pthread_mutex_t forks[NUM_PHILOSOPHERS];
    pthread_cond_t cond_vars[NUM_PHILOSOPHERS];
    pthread_mutex_t mutex;
    int eat_counts[NUM_PHILOSOPHERS];
} dining_table;

/**
 * Prints the current state of all philosophers
 */
void print_states() {
    const char* state_names[] = {"THINKING", "HUNGRY", "EATING"};
    printf("\nCurrent States:\n");
    for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
        printf("Philosopher %d: %s (ate %d times)\n", 
               i, state_names[dining_table.state[i]], dining_table.eat_counts[i]);
    }
    printf("-----------------\n");
}

/**
 * Tests if a philosopher can start eating
 * @param id Philosopher ID (0 to NUM_PHILOSOPHERS-1)
 */
void test_can_eat(int id) {
    int left = (id + NUM_PHILOSOPHERS - 1) % NUM_PHILOSOPHERS;
    int right = (id + 1) % NUM_PHILOSOPHERS;
    
    if (dining_table.state[id] == HUNGRY &&
        dining_table.state[left] != EATING &&
        dining_table.state[right] != EATING) {
        
        dining_table.state[id] = EATING;
        dining_table.eat_counts[id]++;
        pthread_cond_signal(&dining_table.cond_vars[id]);
    }
}

/**
 * Attempts to pick up forks for eating
 * @param id Philosopher ID
 */
void pickup_forks(int id) {
    pthread_mutex_lock(&dining_table.mutex);
    
    dining_table.state[id] = HUNGRY;
    printf("Philosopher %d is now HUNGRY\n", id);
    print_states();
    
    test_can_eat(id);
    while (dining_table.state[id] != EATING) {
        pthread_cond_wait(&dining_table.cond_vars[id], &dining_table.mutex);
    }
    
    pthread_mutex_unlock(&dining_table.mutex);
}

/**
 * Returns forks after eating
 * @param id Philosopher ID
 */
void return_forks(int id) {
    pthread_mutex_lock(&dining_table.mutex);
    
    dining_table.state[id] = THINKING;
    printf("Philosopher %d is now THINKING\n", id);
    
    // Notify neighbors
    int left = (id + NUM_PHILOSOPHERS - 1) % NUM_PHILOSOPHERS;
    int right = (id + 1) % NUM_PHILOSOPHERS;
    test_can_eat(left);
    test_can_eat(right);
    
    print_states();
    pthread_mutex_unlock(&dining_table.mutex);
}

/**
 * Philosopher thread function
 * @param arg Philosopher ID passed as void pointer
 */
void* philosopher(void* arg) {
    int id = *(int*)arg;
    int cycles = 0;
    
    while (cycles < MAX_CYCLES) {
        // Thinking phase
        printf("Philosopher %d is THINKING\n", id);
        sleep(rand() % 3 + 1);
        
        // Hungry phase
        pickup_forks(id);
        
        // Eating phase
        printf("Philosopher %d is EATING\n", id);
        sleep(rand() % 2 + 1);
        
        // Done eating
        return_forks(id);
        cycles++;
    }
    
    return NULL;
}

/**
 * Initializes synchronization primitives and state
 */
void initialize() {
    pthread_mutex_init(&dining_table.mutex, NULL);
    for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
        pthread_mutex_init(&dining_table.forks[i], NULL);
        pthread_cond_init(&dining_table.cond_vars[i], NULL);
        dining_table.state[i] = THINKING;
        dining_table.eat_counts[i] = 0;
    }
}

/**
 * Main function
 */
int main() {
    srand(time(NULL));
    pthread_t philosophers[NUM_PHILOSOPHERS];
    int ids[NUM_PHILOSOPHERS];
    
    initialize();
    
    printf("Starting Dining Philosophers simulation...\n");
    printf("Each philosopher will eat up to %d times.\n", MAX_CYCLES);
    
    // Create philosopher threads
    for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
        ids[i] = i;
        pthread_create(&philosophers[i], NULL, philosopher, &ids[i]);
    }
    
    // Wait for all threads to complete
    for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
        pthread_join(philosophers[i], NULL);
    }
    
    printf("\nFinal Eat Counts:\n");
    for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
        printf("Philosopher %d ate %d times\n", i, dining_table.eat_counts[i]);
    }
    
    // Cleanup resources
    pthread_mutex_destroy(&dining_table.mutex);
    for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
        pthread_mutex_destroy(&dining_table.forks[i]);
        pthread_cond_destroy(&dining_table.cond_vars[i]);
    }
    
    return 0;
}