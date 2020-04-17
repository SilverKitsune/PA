import java.util.concurrent.ThreadLocalRandom;

public final class LazySkipList <T>
{
    public static int MAX_LEVEL = 10;
    final Node<T> head = new Node<T>(Integer.MIN_VALUE);
    final Node<T> tail = new Node<T>(Integer.MAX_VALUE);
    public LazySkipList() {
        for (int i = 0; i < head.next.length; i++) {
            head.next[i] = tail;
        }
    }

    /**
     * Проверка наличия в списке узла с указанным значением. Если элемент есть в списке, то succ - сам этот узел на разных уровнях списка.
     * @param x - искомое значение
     * @param preds - массив узлов предстоящих искомому на всех уровнях списка
     * @param succs - массив узлов которые должны стоять после искомого или сам искомый узел на всех уровнях списка
     * @return максимальный уровень искомого узла
     */
    int find(T x, Node<T>[] preds, Node<T>[] succs)
    {
        int key = x.hashCode();                          //вычисляем хэш-код (ключ) для искомого значения
        int lFound = -1;
        Node<T> pred = head;                             //устанавливаем предществующим значением голову списка
        for (int level = MAX_LEVEL; level >= 0; level--) //Проверяяем все уровни, начиная с самого верхнего
        {
            Node<T> curr = pred.next[level];             //устанавливаем текущее значение
            while (key > curr.key)                       //пока ключ искомого значения больше ключа текущего узла
            { pred = curr; curr = pred.next[level];  }  //движемся дальше по списку

            if (lFound == -1 && key == curr.key)         //Если мы нашли узел с тем же значением и это первый раз
                lFound = level;                          //то устанавливаем максимальный уровень

            preds[level] = pred;                         //Записываем предшествующий и текущий узел в массивы
            succs[level] = curr;
        }
        return lFound;
    }

    /**
     * Добавление узла в список
     * @param x - добавлемое значение
     * @return true  - если элемент добавлен в список
     *         false - если такой элемент уже есть в списке
     */
    boolean add(T x)
    {
        int topLevel = randomLevel();                                     //вычисляем максимальный уровень добовляемого узла
        Node<T>[] preds = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T>[] succs = (Node<T>[]) new Node[MAX_LEVEL + 1];
        while (true)
        {
            int lFound = find(x, preds, succs);                           //проверяем наличие такого узла в списке
            if (lFound != -1)                                             //если узел найден
            {
                Node<T> nodeFound = succs[lFound];
                if (!nodeFound.marked)                                    //если он не был помечен
                {
                    while (!nodeFound.fullyLinked) {}                     // ждем пока он будет связан с другими элементами на всех уровнях списка
                    return false;                                         // и возвращаем false
                }
                continue;                                                 // иначе начинаем заново
            }
            int highestLocked = -1;
            try
            {                                                              //если такой элемент не найден, то переходим к добавлению
                Node<T> pred, succ;
                boolean valid = true;
                for (int level = 0; valid && (level <= topLevel); level++) //проверяем связанность предстоящего узла с последующим на всех уровнях
                {
                    pred = preds[level];
                    succ = succs[level];
                    pred.lock();                                           //блокируем предстоящий узел
                    highestLocked = level;                                 //запоминаем текущий уровень
                    valid = !pred.marked && !succ.marked
                            && pred.next[level]==succ;                     //проверяем связанность
                }
                if (!valid) continue;                                      //если они не связаны, то начинаем все сначала
                Node<T> newNode = new Node<>(x, topLevel);                 //создаем узел с добавляемым значением
                for (int level = 0; level <= topLevel; level++)            //и добавляем его в список на всех уровнях до topLevel
                    newNode.next[level] = succs[level];
                for (int level = 0; level <= topLevel; level++)
                    preds[level].next[level] = newNode;
                    newNode.fullyLinked = true;                            //указываем что этот узел полностью связан на всех уровнях, на которых он есть
                return true;
            }
            finally
            {
                for (int level = 0; level <= highestLocked; level++)       //снимаем блокировку со всех предшествующих узлов
                    preds[level].unlock();
            }
        }
    }

    /**
     * Вычисление случайного уровня для добавляемого узла
     * @return номер уровня
     */
    private int randomLevel()
    {
        int level = 0;
        while (ThreadLocalRandom.current().nextFloat() <= 0.5)
            level++;
        return Math.min(level, MAX_LEVEL);

    }

    /**
     * Удаление элемента из списка
     * @param x - удаляемное значение
     * @return true  - если элемент успешно удален
     *         false - если такого элемента нет
     */
    boolean remove(T x)
    {
         Node<T> victim = null;
         boolean isMarked = false;
         int topLevel = -1;
         Node<T>[] preds = (Node<T>[]) new Node[MAX_LEVEL + 1];
         Node<T>[] succs = (Node<T>[]) new Node[MAX_LEVEL + 1];
         while (true)
         {
             int lFound = find(x, preds, succs);                                //проверяем наличие
             if (lFound != -1) victim = succs[lFound];
             if (isMarked || (lFound != -1 && (victim.fullyLinked
                     && victim.topLevel == lFound && !victim.marked)))          //если узел найден, и мы еще не помечали этот узел и этот узел не помечен и полностью связан на всех его уровнях
             {
                 if (!isMarked)
                 {
                     topLevel = victim.topLevel;
                     victim.lock();                                             //блокируем жертву
                     if (victim.marked)                                         //если жертва уже помечена
                     {
                         victim.unlock();                                       //то снимаем блокировку и возвращаем false
                         return false;
                     }
                     victim.marked = true;                                      //иначе помечаем жертву
                     isMarked = true;                                           //и запоминаем, что пометили ее
                 }
                 int highestLocked = -1;
                 try
                 {
                     Node<T> pred; boolean valid = true;
                     for (int level = 0; valid && (level <= topLevel); level++) //проверяем связанность предстоящего узла с последующим на всех уровнях
                     {
                         pred = preds[level];
                         pred.lock();
                         highestLocked = level;
                         valid = !pred.marked && pred.next[level]==victim;
                     }
                     if (!valid) continue;                                     //если они не связаны, то начинаем все сначала
                     for (int level = topLevel; level >= 0; level--)           //иначе удаляем элемент изз всех уровней списка, на которых он был
                     {
                         preds[level].next[level] = victim.next[level];
                     }
                     victim.unlock();                                          //снимаем блокировку с жертвы
                     return true;
                 }
                 finally
                 {
                     for (int i = 0; i <= highestLocked; i++)                  //снимаем блокировку с предков
                     {
                         preds[i].unlock();
                     }
                 }
             }
             else return false;                                                //если узел с таким значением не был найден, то возвращаем false
         }
    }

    /**
     * Проверка наличия значения в списке
     * @param x - искоое значение
     * @return true  - если такое значение есть в списке
     *         false - иначе
     */
    boolean contains(T x) {
         Node<T>[] preds = (Node<T>[]) new Node[MAX_LEVEL + 1];
         Node<T>[] succs = (Node<T>[]) new Node[MAX_LEVEL + 1];
         int lFound = find(x, preds, succs);                                           //проверяем наличие узла с таким значением
         return (lFound != -1 && succs[lFound].fullyLinked && !succs[lFound].marked);  //если такой есть и он не помечен и полностью связан, то возвращаем true, иначе false
         }

    void print()
    {
        System.out.println("PRINTING SKIPLIST");
        Node<T> current = head;
        for (int i = MAX_LEVEL; i >= 0; i--)
        {
            current = head;
            while (current != tail)
            {
                if (current.item != null) System.out.print(current.item.toString() + " ");
                current = current.next[i];
            }
            System.out.println();
        }
    }
}