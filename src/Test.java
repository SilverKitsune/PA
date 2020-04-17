
public class Test
{
    public static void main(String [] args)
    {
        LazySkipList<Integer> skipList = new LazySkipList<Integer>();
        Thread[] threadsAdding = new Thread[8];
        Thread[] threadsRemoving = new Thread[8];
        Thread[] threadsContains = new Thread[8];
        final int DATA_SAMPLE = 50000;
        for (int i = 0; i < threadsAdding.length; i++)
        {
            int finalI = i;
            threadsAdding[i] = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    for (int j = (DATA_SAMPLE*finalI)/threadsAdding.length; j < (DATA_SAMPLE*(finalI+1))/threadsAdding.length; j++)
                    {
                        skipList.add(j);
                    }
                }
            });
        }
        for (int i = 0; i < threadsRemoving.length; i++)
        {
            int finalI = i;
            threadsRemoving[i] = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    for (int j = (DATA_SAMPLE*finalI)/threadsRemoving.length; j < (DATA_SAMPLE*(finalI+1))/threadsRemoving.length; j++)
                    {
                        skipList.remove(j);
                    }
                }
            });
        }
        for (int i = 0; i < threadsContains.length; i++)
        {
            int finalI = i;
            threadsContains[i] = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    for (int j = (DATA_SAMPLE*finalI)/threadsContains.length; j < (DATA_SAMPLE*(finalI+1))/threadsContains.length; j++)
                    {
                        skipList.contains(j);
                    }
                }
            });
        }
        long start = System.currentTimeMillis();
        for (int i = 0; i < threadsAdding.length; i++)
        {
            threadsAdding[i].start();
        }
        try
        {
            for (int i = 0; i < threadsAdding.length; i++)
            {
                threadsAdding[i].join();
            }
            long end = System.currentTimeMillis();
            long elapsedTime = end - start;
            System.out.println("ELAPSED TIME ADDING: " + elapsedTime);
        }
        catch (InterruptedException e) { System.out.println("MAIN INTERRUPTED");}
        start = System.currentTimeMillis();
        for (int i = 0; i < threadsContains.length; i++)
        {
            threadsContains[i].start();
        }
        try
        {
            for (int i = 0; i < threadsContains.length; i++)
            {
                threadsContains[i].join();
            }
            long end = System.currentTimeMillis();
            long elapsedTime = end - start;
            System.out.println("ELAPSED TIME CONTAINS: " + elapsedTime);
        }
        catch (InterruptedException e) { System.out.println("MAIN INTERRUPTED");}
        start = System.currentTimeMillis();
        for (int i = 0; i < threadsRemoving.length; i++)
        {
            threadsRemoving[i].start();
        }
        try
        {
            for (int i = 0; i < threadsRemoving.length; i++)
            {
                threadsRemoving[i].join();
            }
            long end = System.currentTimeMillis();
            long elapsedTime = end - start;
            System.out.println("ELAPSED TIME REMOVING: " + elapsedTime);
        }
        catch (InterruptedException e) { System.out.println("MAIN INTERRUPTED");}

    }
}
